import os
from datetime import datetime


def create_temp_location(base):
    """

    :param base:
    :return:
    """

    tmp_folder = os.path.join(base, "{:%Y-%m-%d_%H-%M-%S}".format(datetime.utcnow()))

    try:
        os.makedirs(tmp_folder, exist_ok=False)
    except OSError as e:
        print("Cannot create temporary folder {0}".format(tmp_folder))
        print("{!s}".format(e))

        exit(1)

    return tmp_folder
