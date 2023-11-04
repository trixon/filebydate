/*
 * Copyright 2023 Patrik Karlström <patrik@trixon.se>.
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
package se.trixon.filebydate.core;

import java.io.IOException;
import org.openide.util.Exceptions;
import org.openide.windows.InputOutput;

/**
 *
 * @author Patrik Karlström <patrik@trixon.se>
 */
public class Printer {

    private final InputOutput mInputOutput;

    public Printer(InputOutput inputOutput) {
        mInputOutput = inputOutput;
        try {
            mInputOutput.getOut().reset();
            mInputOutput.select();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void errln(String s) {
        if (TaskManager.getInstance().isGui()) {
            mInputOutput.getErr().println(s);
        } else {
            System.out.println(s);
        }
    }

    public void outln(String s) {
        if (TaskManager.getInstance().isGui()) {
            mInputOutput.getOut().println(s);
        } else {
            System.out.println(s);
        }
    }
}
