import os
import shutil
import argparse
import json

import logging

import utilities
import s3

logging.basicConfig(filename='report_utils.log', level=logging.DEBUG)

TMP_LOCATION = os.path.join("", "tmp")

CSV_REPORT_FILE_NAME = "combined-results.csv"
INFO_FILE = "test-info.txt"
VERSION_FILE = "version.log"
STAT_KEYS = [
    "statLabel",
    "numberOfRequestsSent",
    "numberOfBytesSent",
    "numberOfSuccessfulRequests",
    "numberOfFailedRequests",
    "lt_max",
    "lt_min",
    "lt_avg",
    "lt_p98",
    "lt_p90",
    "lt_stddev",
    "durationInSeconds",
    "throughputMBps",
    "reqPerSecondTotal",
    "reqPerSecondSuccess",
    "reqPerSecondFailed",
    "numberOfActiveThreads"
]

"""
Basic algorithm:
Given folder name (or list of folder names):
    - Parse report file (report-<timestamp>.json
        - If single folder was passed: Create CSV report
        - If multiple folders were passed: Create summary CSV report
"""


def get_command_line_arguments():
    parser = argparse.ArgumentParser()

    parser.add_argument('--folders', type=str, required=True, help="Folder list with report location")
    parser.add_argument('--root', type=str, required=False, help="Root folder for passed in folder names", default="")

    return parser.parse_args()


def get_exact_file_name(file_name, file_path):
    """

    :param file_name:
    :param file_path:
    :return:
    """
    for root, dirs, files in os.walk(file_path):
        for name in files:
            logging.debug("Checking {0}".format(name))
            if str(name).startswith(file_name):
                return os.path.join(root, name)

    return None


def get_result_stats(stat_file):
    """

    :param stat_file:
    :return:
    """
    logging.debug("Report file: {0}".format(stat_file))

    with open(stat_file, 'r') as fh:
        stats = json.load(fh)

    return stats


def get_result_info(info_file):
    """

    :param info_file:
    :return:
    """

    result_info = dict()

    if os.path.isfile(info_file):
        with open(info_file, 'r') as fh:
            lines = fh.readlines()

            for line in lines:
                p = line.split(':')
                result_info[p[0]] = str(p[1]).strip()

    return result_info


def get_result_version(version_file):
    """

    :param version_file:
    :return:
    """
    result_version = ""

    if os.path.isfile(version_file):
        with open(version_file, 'r') as fh:
            lines = fh.readlines()

            for line in lines:
                if line.strip().startswith("<responseData"):
                    result_version = line.split('>')[1].split('<')[0]

    return result_version


def make_csv_file(results, csv_file_name="csv_report.csv"):
    """

    :param results:
    :param csv_file_name:
    :return:
    """

    with open(csv_file_name, 'w') as fh:
        for stat in results["stats"]:
            for key in STAT_KEYS:
                fh.write("{0},,{1}\n".format(key, ','.join([str(v) for v in stat[key]])))

            fh.write("\n")


def append_stats(stats, new_stats):
    """
    If stats is an empty array:
        create array of objects based on STAT_KEYS
        each STAT KEY should be an array of values

    If stats is not empty (stats are being appended)
        add stat values to the array of the existing stats
        stats should be matched using statLabel

    :param stats:
    :param new_stats:
    :return:
    """

    if len(stats) == 0:
        for new_stat in new_stats:
            o = dict()
            for key in STAT_KEYS:
                o[key] = list()
                o[key].append(new_stat[key])

            stats.append(o)
    else:
        for new_stat in new_stats:
            for existing_stat in stats:
                if new_stat['statLabel'] == existing_stat['statLabel'][0]:
                    for key in STAT_KEYS:
                        existing_stat[key].append(new_stat[key])

    return stats


def append_info(info, new_info):
    """

    :param info:
    :param new_info:
    :return:
    """

    info.append(new_info)

    return info


def append_version(version, new_version):
    """

    :param version:
    :param new_version:
    :return:
    """
    version.append(new_version)

    return version


def create_csv_report(root, folders):
    """

    :param root:
    :param folders:
    :return:
    """
    results = dict({
        "stats": list(),
        "info": list(),
        "version": list()
    })

    folder_list = str(folders).split(',')

    for folder in folder_list:
        full_folder_path = os.path.join(root, folder)

        logging.debug("Processing: {0}".format(full_folder_path))

        results["stats"] = append_stats(results["stats"],
                                        get_result_stats(get_exact_file_name("report-", full_folder_path)))
        results["info"] = append_info(results["info"],
                                      get_result_info(os.path.join(full_folder_path, INFO_FILE)))
        results["version"] = append_version(results["version"],
                                            get_result_version(os.path.join(full_folder_path, VERSION_FILE)))

    for folder in folder_list:
        make_csv_file(results, csv_file_name=os.path.join(os.path.join(root, folder), CSV_REPORT_FILE_NAME))


def get_files_from_s3(s3_root, folders):
    """
    Download files from S3 into local drive
    :param s3_root:
    :param folders
    :return:
    """
    local_root = utilities.create_temp_location(TMP_LOCATION)

    aws_s3 = s3.AwsS3(aws_region='eu-west-1', s3=s3_root)

    for folder in str(folders).split(','):
        logging.debug("Getting {0} from {1}".format(folder, s3_root))

        dest_folder = os.path.join(local_root, folder)

        os.makedirs(dest_folder)

        aws_s3.download_files_from_s3(sub_key=folder, local_folder=dest_folder)

    return local_root


def upload_report_to_s3(local, remote, folders):
    """
    Uploads report files to S3.
    This function will only upload report file to S3
    :param local:
    :param remote:
    :param folders
    :return:
    """
    aws_s3 = s3.AwsS3(aws_region='eu-west-1', s3=remote)

    for folder in str(folders).split(','):
        source_path = os.path.join(local, folder)
        logging.debug("Uploading {0} to {1}".format(CSV_REPORT_FILE_NAME, folder))
        aws_s3.copy_to_s3(os.path.join(source_path, CSV_REPORT_FILE_NAME), sub_key=folder)


def localize_files(passed_root, folders):
    """
    Check if passed root folder is local or remote.
    If remote (only AWS S3 is supported), get the files and return local root
    If local, do nothing and return passed root folder

    :param passed_root:
    :param folders
    :return:
    """

    if str(passed_root).startswith("s3://"):
        return get_files_from_s3(passed_root, folders)

    return passed_root


def update_remote_files(local, remote, folders):
    """

    :param local:
    :param remote:
    :param folders
    :return:
    """
    if str(remote).startswith("s3://"):
        upload_report_to_s3(local, remote, folders)
        shutil.rmtree(local)


def main():
    """

    :return:
    """
    args = get_command_line_arguments()

    local_root = localize_files(args.root, args.folders)

    create_csv_report(local_root, args.folders)

    update_remote_files(local_root, args.root, args.folders)


if __name__ == "__main__":
    main()
