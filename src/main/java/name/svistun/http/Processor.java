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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Processor {
    private static final Logger log = Logger.getLogger(Processor.class.getSimpleName());

    public Object process(List<Step> steps, Object src) throws ScriptException {
        Object result = src;
        for (Step step : steps) {
            switch (step.getType()) {
                case "jsoup_element_list_get_data":
                    result = getData(((List<Element>) result));
                    break;
                case "get_data_str_list":
                    result = getDataStrList((List<String>) result, step);
                    break;
                case "js":
                        result = js((List<String>) result);
                    break;
                case "remove_line":
                    result = removeLine((List<String>) result, step);
                    break;
                case "replace_line":
                    result = replaceLine((List<String>) result, step);
                    break;
                case "json_doc_select":
                    if (step.getArg(2).equals("doc")) {
                        result = select(step.getArg(1), (Document) result);
                    }
                    break;
                case "trim_lines":
                    result = trimLines((List<String>) result);
                    break;
            }
        }
        return result;
    }

    private List<String> getData(List<Element> elements) {
        List<String> result = new ArrayList<>();
        for (Element element : elements) {
            StringBuilder sb = new StringBuilder();
            for (String line : element.data().split("\\r?\\n")) {
                if (sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(line);
            }
            log.debug("Data: " + System.lineSeparator() + sb.toString());
            result.add(sb.toString());
        }
        return result;
    }

    private Set<String> getDataStrList(List<String> strList, Step step) {
        Set<String> result = new HashSet<>();
        for (String str : strList) {
            Pattern pattern = Pattern.compile(step.getArg(1));
            for (String line : str.split(System.lineSeparator())) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    //todo number of groups take from a config.
                    String ip = matcher.group(1);
                    int port = Integer.parseInt(matcher.group(2));
                    result.add(String.format("%s:%s", ip, port));
                    break;
                }
            }
        }
        return result;
    }

    private List<String> js(List<String> strList) throws ScriptException {
        List<String> result = new ArrayList<>();
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");
        for (String str : strList) {
            String exec = (String) engine.eval(str);
            log.debug(exec);
            result.add(exec);
        }
        return result;
    }


    private List<String> removeLine(List<String> strList, Step step) {
        List<String> result = new ArrayList<>();
        for (String str : strList) {
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
            result.add(sb.toString());
        }
        return result;
    }

    private List<String> replaceLine(List<String> strList, Step step) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(step.getArg(1));
        for (String str : strList) {
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
            result.add(sb.toString());
        }
        return result;
    }

    private Elements select(String request, Document doc) {
        return doc.select(request);
    }

    private List<String> trimLines(List<String> strList) {
        List<String> result = new ArrayList<>();
        for (String str : strList) {
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
            result.add(sb.toString());
        }
        return result;
    }
}
