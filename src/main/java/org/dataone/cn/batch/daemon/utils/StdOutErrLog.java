/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * 
 * $Id$
 */

package org.dataone.cn.batch.daemon.utils;

import java.io.PrintStream;
import org.apache.log4j.Logger;

/**
 *
 * @author rwaltz
 * @see 
 */
public class StdOutErrLog {
private static final Logger logger = Logger.getLogger(StdOutErrLog.class);
     public  void init() {
        System.setOut(createSystemOutLoggingProxy(System.out));
        System.setErr(createSystemErrLoggingProxy(System.err));
    }
    public PrintStream createSystemOutLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                logger.info(string);
            }
        };
    }
    public PrintStream createSystemErrLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                logger.error(string);
            }
        };
    }

}
