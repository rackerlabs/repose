#
#_=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
#Repose
#_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
#Copyright (C) 2010 - 2015 Rackspace US, Inc.
#_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.
#=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
#

#!/usr/bin/python

__author__ = "Dimitry Ushakov"

# This is a DOCUMENTATION stub specific to this module, it extends
# a documentation fragment located in ansible.utils.module_docs_fragments
DOCUMENTATION = '''
---
module: repose
short_description: create (starts) / delete (stop) an instance of Repose
description:
    - creates / deletes a Repose instance and optionally
      waits for it to be 'running'.
options:
  release:
    description:
      - Specifies which release version to deploy and start
    default: null
  git_build:
    description:
      - Whether or not to build repose from source (git)
    default: no
  git_repo:
    description:
      - Which git repository to pull repose from
    default: https://github.com/rackerlabs/repose
  git_branch:
    description:
      - Which git branch to pull repose from
    default: master
  build_tool:
    description:
      - Which build tool to use to build Repose from source
    default: maven
  state:
    description:
      - Indicate desired state of repose
    choices:
      - present
      - absent
    default: present
  wait:
    description:
      - wait for the instance to be in state 'running' before returning
    default: "yes"
    choices:
      - "yes"
      - "no"
  wait_timeout:
    description:
      - how long before wait gives up, in seconds
    default: 300
author: Dimitry Ushakov
'''

EXAMPLES = '''
- name: Build a Repose instance with latest version and wait until started
  gather_facts: False
  tasks:
    - name: Build Repose Instance
      local_action:
        module: repose
      register: repose_module

- name: Build a Repose instance with specific version and do not wait
  hosts: local
  gather_facts: False
  tasks:
    - name: Build Repose Instance
      local_action:
        module: repose
        state: present
        release: 5.0
        wait: no
      register: repose_module

- name: Build a Repose instance from a specific git repo
  hosts: local
  gather_facts: False
  tasks:
    - name: Build Repose Instance
      local_action:
        module: repose
        state: present
        git_build: yes
        git_repo: https://github.com/rackerlabs/repose
        git_branch: fancy-new-feature
        wait: no
      register: repose_module

- name: Shut down a Repose instance
  hosts: local
  gather_facts: False
  tasks:
    - name: Build Repose Instance
      local_action:
        module: repose
        state: absent
      register: repose_module
'''

import shutil
import glob
import os
import commands
import requests


def check_if_repose_is_listening(listen_port, steps):
    steps.append('listen for repose to come back with something good')
    try:
        r = requests.get('http://localhost:%s' % (listen_port))
        if r.status_code < 500:
            steps.append('response: %s' % (r.status_code))
            return True
        else:
            steps.append('response: %s' % (r.status_code))
            return False
    except Exception, e:
        steps.append('response: %s' % (e))
        return False


def started_repose_id():
    output = commands.getoutput('ps aux | grep repose').split('\n')
    for i in output:
        if not 'grep repose' in i:
            return i.split()[1]
    return None


def stop_repose_id(module, pid):
    # stop repose id here
    module.run_command('kill -9 %s' % pid)


def build_with_git(module, git_repo, git_branch, build_tool, wait, wait_timeout,
                   listen_port, check_if_started, steps):

    # TODO: check which os it is (DEB or RPM)
    # 1. get from github (git_repo/git_branch) (git is a dependency)
    # 2. build with maven (mvn is a dependency)
    # 3. take 2 EARs and deploy to /usr/share/repose/filters
    # 4. take JAR and deploy to /usr/share/lib/repose
    # 5. take configurations and parse into /etc/repose
    # 6. start repose
    changed = False
    untouched = dict()
    if check_if_repose_is_listening(listen_port, steps):
        untouched = dict(
            pid=started_repose_id(),
            status='STARTED'
        )
    module.run_command('rm -rf /opt/repose')
    module.run_command('mkdir -p /opt/repose')
    module.run_command('git clone %s /opt/repose' % git_repo,
                       use_unsafe_shell=True, check_rc=True, cwd='/opt/repose')
    module.run_command('git fetch origin +refs/pull/*:refs/remotes/origin/pr/*',
                       use_unsafe_shell=True, check_rc=True, cwd='/opt/repose')
    module.run_command('git checkout -f %s' % git_branch,
                       use_unsafe_shell=True, check_rc=True, cwd='/opt/repose')
    if build_tool == "maven":
        rc, out, err = module.run_command('mvn clean install -DskipTests -Pbuild-system-packages', cwd='/opt/repose', check_rc=False)
        for ext_file in glob.glob(r'/opt/repose/repose-aggregator/installation/'
                              r'deb/repose/target/'
                              r'repose-*SNAPSHOT.deb'):
            rc, out, err = module.run_command('dpkg -i %s' % ext_file, cwd='/opt/repose', check_rc=True)
            steps.append('result from repose: %s, %s, %s' % (rc, out, err))
        for ext_file in glob.glob(r'/opt/repose/repose-aggregator/installation/'
                              r'deb/repose-filter-bundle/target/'
                              r'repose-filter-bundle-*SNAPSHOT.deb'):
            rc, out, err = module.run_command('dpkg -i %s' % ext_file, cwd='/opt/repose', check_rc=True)
            steps.append('result from repose-filter-bundle: %s, %s, %s' % (rc, out, err))
        for ext_file in glob.glob(r'/opt/repose/repose-aggregator/installation/'
                              r'deb/repose-extensions-filter-bundle/target/'
                              r'repose-extensions-filter-bundle-*SNAPSHOT.deb'):
            rc, out, err = module.run_command('dpkg -i %s' % ext_file, cwd='/opt/repose', check_rc=True)
            steps.append('result from repose-extensions-filter-bundle: %s, %s, %s' % (rc, out, err))
    elif build_tool == "gradlew":
        rc, out, err = module.run_command('./gradlew buildDeb -Prelease', cwd='/opt/repose', check_rc=False)
        for ext_file in glob.glob(r'/opt/repose/repose-aggregator/artifacts/'
                                  r'valve/build/distributions/'
                                  r'repose*.deb'):
            rc, out, err = module.run_command('dpkg -i %s' % ext_file, cwd='/opt/repose', check_rc=True)
            steps.append('result from repose: %s, %s, %s' % (rc, out, err))
        for ext_file in glob.glob(r'/opt/repose/repose-aggregator/artifacts/'
                                  r'filter-bundle/build/distributions/'
                                  r'repose-filter-bundle*.deb'):
            rc, out, err = module.run_command('dpkg -i %s' % ext_file, cwd='/opt/repose', check_rc=True)
            steps.append('result from repose-filter-bundle: %s, %s, %s' % (rc, out, err))
        for ext_file in glob.glob(r'/opt/repose/repose-aggregator/artifacts/'
                                  r'extensions-filter-bundle/build/distributions/'
                                  r'repose-extensions-filter-bundle*.deb'):
            rc, out, err = module.run_command('dpkg -i %s' % ext_file, cwd='/opt/repose', check_rc=True)
            steps.append('result from repose-extensions-filter-bundle: %s, %s, %s' % (rc, out, err))
        for ext_file in glob.glob(r'/opt/repose/repose-aggregator/artifacts/'
                                  r'experimental-filter-bundle/build/distributions/'
                                  r'repose-experimental-filter-bundle*.deb'):
            rc, out, err = module.run_command('dpkg -i %s' % ext_file, cwd='/opt/repose', check_rc=True)
            steps.append('result from repose-experimental-filter-bundle: %s, %s, %s' % (rc, out, err))
    steps.append('result from %s: %s, %s' % (build_tool, rc, err))

    # start repose from repose
    if check_if_started:
        steps.append('we also want to start it')
        start_repose(module, listen_port, wait, wait_timeout, steps)
    else:
        success = dict(
            status='INSTALLED',
            steps=steps
        )
        results = {
            'changed': True,
            'action': 'installed',
            'success': success,
            'error': None
        }
        result(module, results, None)


def build_with_release(module, release, package, wait, wait_timeout,
                       listen_port, check_if_started, steps):

    # 1. check which os it is (DEB or RPM)
    # 2. install repose
    # 3. call repose to start repose
    steps.append('in build with release method')
    steps.append('check if repose is listening (so that we do not have to do it again)')
    untouched = dict()
    changed = False
    if check_if_repose_is_listening(listen_port, steps):
        steps.append('repose is started on %s' % started_repose_id())
        untouched = dict(
            pid=started_repose_id(),
            status='STARTED'
        )
    else:
        # TO DO: we should check if Repose is already installed before trying to install it (to be idempotent)
        changed = True
        steps.append('not started. check package')
        if package in ('Ubuntu', 'Debian'):
            steps.append('do apt-get install')
            (rc, out, err) = module.run_command('export DEBIAN_FRONTEND=noninteractive; apt-get update', check_rc=True, use_unsafe_shell=True)
            steps.append("Just ran update: %s, %s, %s" % (rc, out, err))
            (rc, out, err) = module.run_command("sudo wget -O - http://repo.openrepose.org/debian/pubkey.gpg | sudo apt-key add -", check_rc=True, use_unsafe_shell=True)
            steps.append("Just updated apt-key: %s, %s, %s" % (rc, out, err))
            (rc, out, err) = module.run_command('sudo sh -c \'echo "deb http://repo.openrepose.org/debian stable main" > /etc/apt/sources.list.d/openrepose.list\'', use_unsafe_shell=True, check_rc=True)
            steps.append("Just added sources: %s, %s, %s" % (rc, out, err))
            (rc, out, err) = module.run_command('export DEBIAN_FRONTEND=noninteractive; apt-get update', check_rc=True, use_unsafe_shell=True)
            steps.append("Just ran update: %s, %s, %s" % (rc, out, err))
            (rc, out, err) = module.run_command('export DEBIAN_FRONTEND=noninteractive; apt-get install '
                                                'repose=%s '
                                                'repose-filter-bundle=%s '
                                                'repose-extensions-filter-bundle=%s '
                                                'repose-experimental-filter-bundle=%s '
                                                '-y --force-yes -q' %
                                                (release, release, release, release),
                                                use_unsafe_shell=True, check_rc=True)
            steps.append("Just installed all repose: %s, %s, %s" % (rc, out, err))
            # start repose from repose
            if check_if_started:
                steps.append('we also want to start it')
                start_repose(module, listen_port, wait, wait_timeout, steps)
            else:
                steps.append('we do not wait to wait if started')
                success = dict(
                    status='INSTALLED',
                    steps=steps
                )
                results = {
                    'changed': True,
                    'action': 'installed',
                    'success': success,
                    'error': None
                }
                result(module, results, None)
        elif package == ('RedHat', 'Fedora', 'CentOS'):
            steps.append('do yum install')
            module.run_command('sudo yum update')
            module.run_command('sudo wget -O /etc/yum.repos.d/openrepose.repo http://repo.openrepose.org/el/openrepose.repo')
            module.run_command('sudo yum update')
            module.run_command('sudo yum install '
                               'repose=%s '
                               'repose-filter-bundle=%s '
                               'repose-extensions-filter-bundle=%s '
                               'repose-experimental-filter-bundle=%s '
                               '-y --force-yes -q' %
                               (release, release, release, release),
                               use_unsafe_shell=True, check_rc=True)
            # start repose from repose
            # start repose from repose
            if check_if_started:
                steps.append('we also want to start it')
                start_repose(module, listen_port, wait, wait_timeout, steps)
            else:
                success = dict(
                    status='INSTALLED',
                    steps=steps
                )
                results = {
                    'changed': True,
                    'action': 'installed',
                    'success': success,
                    'error': None
                }
                result(module, results, None)
        else:
            steps.append('invalid package %s' % package)
            module.fail_json(msg='Invalid package specified: %s' & package)

    steps.append('validate repose installation')
    validate_repose(module, changed, untouched, listen_port, steps)


def start_repose(module, listen_port, wait, wait_timeout, steps, service_command='start'):
    success = []
    error = []
    timeout = []
    steps.append('trying to %s repose' % service_command)
    # TODO: Add support for systemD
    module.run_command('service repose %s' % service_command)
    changed = False
    if wait:
        steps.append('we are waiting for %s' % wait_timeout)
        end_time = time.time() + wait_timeout
        infinite = wait_timeout == 0
        while infinite or time.time() < end_time:
            steps.append('current time: %s, end time: %s' % (time.time(), end_time))
            if check_if_repose_is_listening(listen_port, steps):
                changed = True
                break
            else:
                time.sleep(5)

    if changed:
        steps.append('repose is started on %s' % started_repose_id())
        success = dict(
            pid=started_repose_id(),
            status='STARTED',
            steps=steps
        )
    else:
        steps.append('not started')
        error = dict(
            status='FAILED',
            steps=steps
        )
    results = {
        'changed': changed,
        'action': 'start',
        'pid': started_repose_id(),
        'success': success,
        'error': error,
        'timeout': timeout
    }
    result(module, results, error)


def result(module, results, error):
    if error:
        results['msg'] = 'Failed to start repose'

    if 'msg' in results:
        module.fail_json(**results)
    else:
        module.exit_json(**results)


def validate_repose(module, changed, untouched, listen_port, steps):
    success = []
    error = []
    timeout = []

    steps.append('check if repose is started')
    steps.append('OUTPUT OF LSOF -I: ')
    steps.append(commands.getoutput('lsof -i | grep repose'))
    steps.append(len(commands.getoutput('lsof -i | grep repose').split('\n')))
    steps.append('OUTPUT OF PS AUX: ')
    steps.append(commands.getoutput('ps aux | grep repose'))
    steps.append(len(commands.getoutput('ps aux | grep repose').split('\n')))
    if check_if_repose_is_listening(listen_port, steps):
        steps.append('repose is started on %s' % started_repose_id())
        success = dict(
            pid=started_repose_id(),
            status='STARTED',
            steps=steps
        )
    else:
        steps.append('not started')
        error = dict(
            status='FAILED',
            steps=steps
        )

    results = {
        'changed': changed,
        'action': 'start',
        'pid': started_repose_id(),
        'success': success,
        'error': error,
        'timeout': timeout,
        'untouched': untouched
    }

    if error:
        results['msg'] = 'Failed to build repose'

    if 'msg' in results:
        module.fail_json(**results)
    else:
        module.exit_json(**results)


def delete(module, wait, wait_timeout):
    changed = False
    pid = started_repose_id()

    try:
        stop_repose_id(module, pid)
    except Exception, e:
        module.fail_json(msg=e.message)
    else:
        changed = True

    # If requested, wait for server deletion
    if wait:
        end_time = time.time() + wait_timeout
        infinite = wait_timeout == 0
        while infinite or time.time() < end_time:
            if started_repose_id():
                time.sleep(5)
            else:
                break

    if check_if_repose_is_listening(listen_port, steps):
        error = dict(
            status='FAILED'
        )
    else:
        success = dict(
            pid=started_repose_id(),
            status='STARTED'
        )

    results = {
        'changed': changed,
        'action': 'delete',
        'success': success,
        'error': error
    }

    if error:
        results['msg'] = 'Failed to delete repose'

    if 'msg' in results:
        module.fail_json(**results)
    else:
        module.exit_json(**results)


# repose module
def repose(module, state, release, git_build, git_repo, git_branch, build_tool,
           wait, wait_timeout, listen_port, steps):
    # act on the state
    steps.append('state: %s' % state)
    # state options are: installed, started, restarted, stopped, present, absent
    if state == 'present':
        # check if git_build is set to true
        if git_build:
            # try to pull from git_repo/git_branch
            steps.append('we are in git build')
            build_with_git(module, git_repo, git_branch, build_tool, wait,
                           wait_timeout, listen_port, True, steps)
            pass
        else:
            # build release
            # first get the operating system to either build via deb or rpm
            steps.append('we are in release build')
            (package, _, _) = platform.linux_distribution()
            steps.append('building on %s' % package)
            if release:
                steps.append('release build here')
                build_with_release(module, release, package, wait, wait_timeout,
                                   listen_port, True, steps)
            else:
                steps.append('oops, still doing the git build')
                build_with_git(module, git_repo, git_branch, build_tool, wait,
                               wait_timeout, listen_port, True, steps)

    elif state == 'absent':
        delete(module, wait, wait_timeout)

    elif state == 'installed':
        # check if git_build is set to true
        if git_build:
            # try to pull from git_repo/git_branch
            steps.append('we are in git build')
            build_with_git(module, git_repo, git_branch, build_tool, wait,
                           wait_timeout, listen_port, False, steps)
        else:
            # build release
            # first get the operating system to either build via deb or rpm
            steps.append('we are in release build')
            (package, _, _) = platform.linux_distribution()
            steps.append('building on %s' % package)
            if release:
                steps.append('release build here')
                build_with_release(module, release, package, wait, wait_timeout,
                                   listen_port, False, steps)
            else:
                steps.append('oops, still doing the git build')
                build_with_git(module, git_repo, git_branch, build_tool, wait,
                               wait_timeout, listen_port, False, steps)

    elif state == 'started':
        start_repose(module, listen_port, wait, wait_timeout, steps)

    elif state == 'restarted':
        start_repose(module, listen_port, wait, wait_timeout, steps, 'restart')


# this is the starting point of the module!
def main():
    module = AnsibleModule(
        argument_spec = dict(
            release = dict(),
            git_build = dict(default=False, type='bool'),
            git_repo = dict(
                default = 'https://github.com/rackerlabs/repose'),
            git_branch = dict(default='master'),
            build_tool = dict(default='maven', choices=['maven', 'gradlew']),
            state = dict(default='present', choices=['installed', 'started',
                                                   'restarted', 'stopped',
                                                   'present', 'absent']),
            wait = dict(default=False, type='bool'),
            wait_timeout = dict(default=300),
            listen_port = dict(default=7070)
        )
    )

    release = module.params.get('release')
    git_build = module.params.get('git_build')
    git_repo = module.params.get('git_repo')
    git_branch = module.params.get('git_branch')
    build_tool = module.params.get('build_tool')
    state = module.params.get('state')
    wait = module.params.get('wait')
    wait_timeout = int(module.params.get('wait_timeout'))
    listen_port = int(module.params.get('listen_port'))

    steps = []

    steps.append('initialize in state: %s, release: %s, git_buid: %s, git_repo:%s, '
                 'git_branch: %s, build_tool: %s, wait: %s, wait_timeout: %s, '
                 'listen_port: %s' % (state, release, git_build, git_repo,
                 git_branch, build_tool, wait, wait_timeout, listen_port))
    repose(module, state, release, git_build, git_repo, git_branch, build_tool,
           wait, wait_timeout, listen_port, steps)


# import module snippets
from ansible.module_utils.basic import *

### invoke the module
if __name__ == '__main__':
    main()
