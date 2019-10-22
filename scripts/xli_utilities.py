import re


def s3_path_combine(*args):
    ret_val = ('/'.join(args)).replace('//', '/').replace('https:/', 'https://')
    ret_val = ret_val[1:] if ret_val.startswith('/') else ret_val

    return ret_val


def do_match(regex, string, group_idx=1):
    match = re.search(regex, string)
    if match:
        return match.group(group_idx)

    return ""
