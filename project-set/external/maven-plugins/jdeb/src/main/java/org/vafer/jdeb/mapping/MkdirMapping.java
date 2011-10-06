/*
 * Copyright 2011 zinic.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vafer.jdeb.mapping;

import org.apache.tools.tar.TarEntry;

/**
 *
 * @author zinic
 */
public class MkdirMapping implements Mapper {

    private String prefix;
    private int uid;
    private int gid;
    private String user;
    private String group;
    private int dirMode;

    public static int toMode(String modeString) {
        int mode = -1;
        if (modeString != null && modeString.length() > 0) {
            mode = Integer.parseInt(modeString, 8);
        }
        return mode;
    }

    public MkdirMapping(String prefix, int uid, int gid, String user, String group, String dirMode) {
        this.prefix = prefix;
        this.uid = uid;
        this.gid = gid;
        this.user = user;
        this.group = group;
        this.dirMode = toMode(dirMode);
    }

    public TarEntry map(TarEntry entry) {
        final TarEntry newEntry = new TarEntry(prefix);
        
        newEntry.setGroupId(gid < 0 ? entry.getGroupId() : gid);
        newEntry.setGroupName(group == null ? entry.getGroupName() : group);
        newEntry.setUserId(uid < 0 ? entry.getUserId() : uid);
        newEntry.setUserName(user == null ? entry.getUserName() : user);
        newEntry.setMode(dirMode < 0 ? entry.getMode() : dirMode);

        return newEntry;
    }
}
