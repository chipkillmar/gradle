/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.launcher.daemon.server.health;

public class DaemonHealthMessages {

    static final String BUILDING_IN_NEW_DAEMON = "Starting build in new daemon [memory: %s]";
    static final String BUILDING_IN_EXISTING_DAEMON = "Starting %s build in daemon [uptime: %s, performance: %s%%, memory: %s%% of %s]";

    public static boolean lineMatches(String line) {
        return line.matches(wildcard(BUILDING_IN_NEW_DAEMON)) || line.matches(wildcard(BUILDING_IN_EXISTING_DAEMON));
    }

    //replace %s with .+ and escape []
    private static String wildcard(String message) {
        return message.replaceAll("[\\[\\]]", "\\\\$0").replaceAll("%s", ".+").replaceAll("%%", "%").concat("\\s*");
    }
}
