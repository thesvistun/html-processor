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

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Processor {
    private static final Logger log = Logger.getLogger(Processor.class.getSimpleName());

    public List<String> process(List<Step> steps, Object src) throws ProcessorException {
        Object result = src;
        for (Step step : steps) {
            if (result instanceof List) {
                List<Object> _result = new ArrayList<>();
                for (Object obj : (List) result ) {
                    if (obj instanceof Element) {
                        switch (step.getType()) {
                            case "jsoup_elements-get_data-strings":
                                _result.add(getData((Element) obj));
                                break;
                            default:
                                throw new ProcessorException(String.format("Step %s does not exist for %s input as input",
                                        step.getType(),
                                        obj.getClass()));
                        }
                    } else if (obj instanceof String) {
                        switch (step.getType()) {
                            case "strings-get_data-strings":
                                String str = getDataStrList((String) obj, step);
                                if (null == str) continue;
                                _result.add(str);
                                break;
                            case "strings-js-strings":
                                try {
                                    _result.add(js((String) obj));
                                } catch (ScriptException e) {
                                    throw new ProcessorException(String.format("%s: %s",
                                            e.getClass().getSimpleName(),
                                            e.getMessage()));
                                }
                                break;
                            case "strings-remove_line-strings":
                                _result.add(removeLine((String) obj, step));
                                break;
                            case "strings-replace_line-strings":
                                _result.add(replaceLine((String) obj, step));
                                break;
                            case "strings-trim_lines-strings":
                                _result.add(trimLines((String) obj));
                                break;
                            default:
                                throw new ProcessorException(String.format("Step %s does not exist for %s input as input",
                                        step.getType(),
                                        obj.getClass()));
                        }
                    }
                }
                result = _result;
            } else {
                if (result instanceof Document) {
                    switch (step.getType()) {
                        case "doc-select-elements":
                            if (step.getArg(2).equals("doc")) {
                                result = select(step.getArg(1), (Document) result);
                            }
                            break;
                        default:
                            throw new ProcessorException(String.format("Step %s does not exist for %s input as input",
                                    step.getType(),
                                    result.getClass()));
                    }
                }
            }
        }
        return (result instanceof List && ((List) result).get(0) instanceof String) ? (List) result : null;
    }

    private String getData(Element element) {
        StringBuilder sb = new StringBuilder();
        for (String line : element.data().split("\\r?\\n")) {
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(line);
        }
        log.debug("Data: " + System.lineSeparator() + sb.toString());
        return sb.toString();
    }

    private String getDataStrList(String str, Step step) {
        Pattern pattern = Pattern.compile(step.getArg(1));
        for (String line : str.split(System.lineSeparator())) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                //todo number of groups take from a config.
                String ip = matcher.group(1);
                int port = Integer.parseInt(matcher.group(2));
                return String.format("%s:%s", ip, port);
            }
        }
        return null;
    }

    private String js(String str) throws ScriptException {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");
        String exec = (String) engine.eval(str);
        log.debug(exec);
        return exec;
    }


    private String removeLine(String str, Step step) {
        StringBuilder sb = new StringBuilder();
        for (String line : str.split(System.lineSeparator())) {
            line = line.trim();
            if (line.matches(step.getArg(1))) {
                log.debug("Removing: " + line);
                continue;
            }
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(line);
        }
        return sb.toString();
    }

    private String replaceLine(String str, Step step) {
        Pattern pattern = Pattern.compile(step.getArg(1));
            StringBuilder sb = new StringBuilder();
            for (String line : str.split(System.lineSeparator())) {
                line = line.trim();
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    StringBuilder lineSb = new StringBuilder();
                    for (Object replacementItem : step.getReplacement()) {
                        if (replacementItem instanceof String) {
                            lineSb.append((String) replacementItem);
                        } else if (replacementItem instanceof Step.MatchGroup) {
                            lineSb.append(matcher.group(((Step.MatchGroup) replacementItem).getNumber()));
                        }
                    }
                    if (sb.length() > 0) {
                        sb.append(System.lineSeparator());
                    }
                    log.debug("Replacing: '" + line + "' -> '" + lineSb.toString() + "'");
                    sb.append(lineSb.toString());
                } else {
                    if (sb.length() > 0) {
                        sb.append(System.lineSeparator());
                    }
                    sb.append(line);
                }
            }
            log.debug(sb.toString());
        return sb.toString();
    }

    private Elements select(String request, Document doc) {
        return doc.select(request);
    }

    private String trimLines(String str) {
            StringBuilder sb = new StringBuilder();
            for (String line : str.split(System.lineSeparator())) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(line);
            }
            log.debug("Result: " + System.lineSeparator() + sb.toString());
        return sb.toString();
    }
}
