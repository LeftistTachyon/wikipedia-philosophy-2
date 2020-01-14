package com.github.leftisttachyon;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

/**
 * The main class
 *
 * @author Jed Wang
 */
@Slf4j
public class Main {
    /**
     * # of pages to visit
     */
    private static final int PAGES_TO_VISIT = 100;

    /**
     * # of cores on this computer
     */
    private static final int CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Keeps track of how many pages to go
     */
    private static int cntr = PAGES_TO_VISIT;

    /**
     * # of threads finished executing
     */
    private static int finished = 0;

    /**
     * A DocumentRequester for requesting documents
     */
    private static DocumentRequester dr;

    /**
     * All keeping track of statistics
     */
    private static double totalTime = 0;
    private static int max = -1, toPhil = 0;
    private static String title = null;
    private static double lastStart = 0;

    /**
     * A cache of pages so I don't have to repeat myself
     */
    private static final HashMap<String, Integer> PAGE_CACHE = new HashMap<>();

    static {
        PAGE_CACHE.put("Philosophy", 0);
    }

    /**
     * Updates statistics
     *
     * @param title  the title of the page
     * @param hops   the number of hops
     * @param millis the amount of milliseconds taken to get there
     */
    private static void newPage(String title, int hops, double millis) {
        if (hops > -1) {
            toPhil++;
        }
        if (hops > max) {
            Main.title = title;
            max = hops;
        }
        totalTime += millis;
    }

    /**
     * The main method
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        dr = new DocumentRequester();
        Thread thread = new Thread(dr);
        thread.setDaemon(true);
        thread.start();

        int coresToUse = /*Math.max(CORES - 2, 1)*/ 1;
        log.info("Detected {} cores; creating {} thread(s).", CORES, coresToUse);

        lastStart = System.nanoTime();
        for (int i = 0; i < coresToUse; i++) {
            new Thread(() -> {
                while (cntr > 0) {
                    Document tempD = dr.request("https://en.wikipedia.org/wiki/Special:Random");
                    int hops = hopsToPhilosophy(tempD);
                    double total = System.nanoTime() - lastStart;
                    total /= 1_000_000;
                    String title_ = tempD.selectFirst("h1#firstHeading").text();
                    if (cntr > 0) {
                        newPage(title_, hops, total);
                    } else {
                        break;
                    }
                    cntr--;
                    if (log.isInfoEnabled()) {
                        String line = String.format("%-100s%.3f ms", title_ + ": " + hops + " hops", total);
                        log.info(line);
                    }
                    lastStart = System.nanoTime();
                }
                if (++finished == coresToUse) {
                    printStatistics();
                }
            }).start();
        }
    }

    /**
     * Loops through a single random page
     *
     * @param current the Document to start at
     * @return how many hops to Philosophy, -1 if it doesn't
     */
    public static int hopsToPhilosophy(Document current) {
        LinkedList<String> path = new LinkedList<>();
        int output = 0;
        String title;
        while (!PAGE_CACHE.containsKey(title = current.getElementById("firstHeading").text())) {
            log.trace("Current title: {}", title);

            if (path.contains(title)) {
                synchronized (PAGE_CACHE) {
                    for (String s : path) {
                        PAGE_CACHE.put(s, -1);
                    }
                }
                return ~output;
            }

            path.add(title);

            current.select("table").remove();
            current.select("sup").remove();

            Element bodyStuff = current.getElementById("bodyContent");
            Elements parags = bodyStuff.getElementsByTag("p");
            Element toGo = null;
            if (parags.select("a[href]").isEmpty()) {
                Elements listElements = bodyStuff.getElementsByTag("li");
                outer:
                for (Element listElement : listElements) {
                    String temp = listElement.outerHtml();
                    temp = temp.replace("<br>", "");
                    TreeMap<Integer, Integer> map = map(temp);
                    Document li = Jsoup.parse(temp);
                    Elements links = li.select("a[href]");
                    if (links.isEmpty()) {
                        continue;
                    }
                    for (Element link : links) {
                        if (link.parent().id().equals("coordinates")) {
                            continue outer;
                        }
                        String outer = link.outerHtml();
                        if (map.get(map.floorKey(temp.indexOf(outer))) != 0) {
                            continue;
                        }
                        String linkHref = link.attr("href");
                        if (linkHref.startsWith("/") && !linkHref.startsWith("#") && !linkHref.contains(":")
                                && !linkHref.contains("action=edit")) {
                            toGo = link;
                            break outer;
                        }
                    }
                }
            } else {
                outer:
                for (Element parag : parags) {
                    String temp = parag.outerHtml();
                    temp = temp.replace("<br>", "");
                    TreeMap<Integer, Integer> map = map(temp);
                    Document p = Jsoup.parse(temp);
                    Elements links = p.select("a[href]");
                    if (links.isEmpty()) {
                        continue;
                    }
                    for (Element link : links) {
                        if (link.parent().id().equals("coordinates")) {
                            continue outer;
                        }
                        String outer = link.outerHtml();
                        if (map.get(map.floorKey(temp.indexOf(outer))) != 0) {
                            continue;
                        }
                        String linkHref = link.attr("href");
                        if (linkHref.startsWith("/") && !linkHref.startsWith("#") && !linkHref.contains(":")
                                && !linkHref.contains("action=edit")) {
                            toGo = link;
                            break outer;
                        }
                    }
                }
                if (toGo == null) {
                    Elements listElements = bodyStuff.select("li");
                    outer:
                    for (Element listElement : listElements) {
                        String temp = listElement.outerHtml();
                        temp = temp.replace("<br>", "");
                        TreeMap<Integer, Integer> map = map(temp);
                        Document li = Jsoup.parse(temp);
                        Elements links = li.select("a[href]");
                        if (links.isEmpty()) {
                            continue;
                        }
                        for (Element link : links) {
                            if (link.parent().id().equals("coordinates")) {
                                continue outer;
                            }
                            String outer = link.outerHtml();
                            if (map.get(map.floorKey(temp.indexOf(outer))) != 0) {
                                continue;
                            }
                            String linkHref = link.attr("href");
                            if (linkHref.startsWith("/") && !linkHref.startsWith("#") && !linkHref.contains(":")
                                    && !linkHref.contains("action=edit")) {
                                toGo = link;
                                break outer;
                            }
                        }
                    }
                }
            }
            if (toGo == null) {
                synchronized (PAGE_CACHE) {
                    for (String s : path) {
                        PAGE_CACHE.put(s, -1);
                    }
                }
                return ~output;
            }
            current = dr.request("https://en.wikipedia.org" + toGo.attr("href"));
            output++;
        }
        Iterator<String> iter = path.iterator();
        synchronized (PAGE_CACHE) {
            int temp = PAGE_CACHE.get(title), size = path.size();
            log.trace("Found {} with path 'o {}", title, temp);
            log.trace("temp: {}", temp);
            log.trace("Path length: {}", size);
            for (int i = 0; iter.hasNext(); i++) {
                String next = iter.next();
                int val = temp + size - i;
                log.trace("{}: {}", next, val);
                PAGE_CACHE.put(next, val);
            }
        }
        return PAGE_CACHE.get(path.getFirst());
    }

    /**
     * Map the thing
     *
     * @param document String thing
     * @return Map
     */
    public static TreeMap<Integer, Integer> map(final String document) {
        return map(document, "(", ")");
    }

    /**
     * Map the thing
     *
     * @param document String thing
     * @param openS    the opening symbol
     * @param closeS   the closing symbol
     * @return Map
     */
    public static TreeMap<Integer, Integer> map(final String document,
                                                final String openS, final String closeS) {
        String copy = document;
        TreeMap<Integer, Integer> parentheses = new TreeMap<>();
        parentheses.put(0, 0);
        int open, close, add = 0, in = 0;
        for (; ; ) {
            open = copy.indexOf(openS);
            close = copy.indexOf(closeS);
            if (open == -1) {
                if (close == -1) {
                    // nothing works
                    break;
                } else {
                    // only closing
                    // temp = close;
                    if (in > 0) {
                        parentheses.put(add + close,
                                --in);
                    }
                    close++;
                    add += close;
                    copy = copy.substring(close);
                }
            } else {
                if (close == -1) {
                    // only opening
                    // temp = open;
                    parentheses.put(add + open,
                            ++in);
                    open++;
                    add += open;
                    copy = copy.substring(open);
                } else {
                    // both
                    if (open < close) {
                        parentheses.put(add + open,
                                ++in);
                        open++;
                        add += open;
                        copy = copy.substring(open);
                    } else {
                        if (in > 0) {
                            parentheses.put(add + close,
                                    --in);
                        }
                        close++;
                        add += close;
                        copy = copy.substring(close);
                    }
                }
            }
        }
        return parentheses;
    }

    /**
     * Prints out the statistics.
     */
    private static void printStatistics() {
        if (log.isInfoEnabled()) {
            log.info("");
            log.info("Max hops: {} hops - {}", max, title);
            String avgTime = String.format("%.3f", totalTime / PAGES_TO_VISIT);
            log.info("Avg time: {} ms", avgTime);
            String percent = String.format("%.2f", ((double) toPhil * 100) / PAGES_TO_VISIT);
            log.info("% to philosophy: {}%", percent);
        }
    }
}
