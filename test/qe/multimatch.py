
import validator


def check_sspnn(protocol, host, port, pbr):
    roles_and_responses = {
        'role-0': 403,
        'role-1': 405,
        'role-2': 405,
        'role-3': 200,
        'role-4': 404,
        'role-5': 404,
        'role-2,role-3': 405,
        'role-3,role-4': 200
        }
    return validator.check_responses(host, 'multimatch/sspnn',
                                     roles_and_responses,
                                     protocol=protocol, port=port,
                                     print_bad_responses=pbr)


def check_p(protocol, host, port, pbr):
    return validator.check_responses(host, 'multimatch/p',
                                     {'role-0': 403, 'role-1': 200},
                                     protocol=protocol, port=port,
                                     print_bad_responses=pbr)


def check_f(protocol, host, port, pbr):
    return validator.check_responses(host, 'multimatch/f',
                                     {'role-0': 403, 'role-1': 405},
                                     protocol=protocol, port=port,
                                     print_bad_responses=pbr)


def check_mssfsffpnn(protocol, host, port, pbr):
    roles_and_responses = {
        'role-0': 403,
        'role-1': 405,
        'role-2': 405,
        'role-3': 405,
        'role-4': 405,
        'role-5': 405,
        'role-6': 405,
        'role-7': 200,
        'role-8': 404,
        'role-9': 404,
        'role-3,role-5,role-6,role-7': 200,
        'role-3,role-5,role-6': 405,
        'role-7,role-8': 200
        }
    return validator.check_responses(host, 'multimatch/mssfsffpnn',
                                     roles_and_responses,
                                     protocol=protocol, port=port,
                                     print_bad_responses=pbr)


def check_mp(protocol, host, port, pbr):
    return validator.check_responses(host, 'multimatch/mp',
                                     {'role-0': 403, 'role-1': 200},
                                     protocol=protocol, port=port,
                                     print_bad_responses=pbr)


def check_mf(protocol, host, port, pbr):
    return validator.check_responses(host, 'multimatch/mf',
                                     {'role-0': 403, 'role-1': 405},
                                     protocol=protocol, port=port,
                                     print_bad_responses=pbr)
