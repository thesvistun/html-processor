/*
 * MIT License
 *
 * Copyright (c) 2017 Svistunov Aleksey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package name.svistun.http;

import java.util.LinkedList;
import java.util.List;

public class Step {
    private List<String> args;
    private List<Object> replacement;
    private String type;

    public Step(String type, List<String> args) {
        this.args = args;
        this.type = type;
        replacement = new LinkedList<>();
        if (type.equals("replace_line")) {
            char[] replacementArr = args.get(1).toCharArray();
            boolean start = false;
            boolean slash = false;
            String str = "";
            for (char currentChar : replacementArr) {
                // "
                if (currentChar == '"') {
                    if (start) {
                        if (slash) {
                            str = str.concat(String.valueOf(currentChar));
                            slash = false;
                        } else {
                            replacement.add(str);
                            str = "";
                            start = false;
                        }
                    } else {
                        start = true;
                    }
                // \
                } else if (currentChar == '\\') {
                    slash = true;
                // char
                } else {
                    if (start) {
                        if (slash) {
                            str = str.concat(String.valueOf('\\'));
                            slash = false;
                        }
                        str = str.concat(String.valueOf(currentChar));
                    } else {
                        replacement.add(new MatchGroup(Integer.parseInt(String.valueOf(currentChar))));
                    }
                }
            }
        }
    }

    String getArg(int number) {
        return args.get(number - 1);
    }

    List<Object> getReplacement() {
        return replacement;
    }

    String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Step{" +
                "type='" + type + '\'' +
                '}';
    }

    class MatchGroup {
        private int number;

        MatchGroup(int number) {
            this.number = number;
        }

        int getNumber() {
            return number;
        }
    }
}
