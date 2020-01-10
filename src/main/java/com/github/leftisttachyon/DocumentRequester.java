package com.github.leftisttachyon;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A class that gives Documents from a BlockingQueue
 *
 * @author Jed Wang
 */
@Slf4j
public class DocumentRequester implements Runnable {

    /**
     * A {@code LinkedBlockingQueue} that this class is based around
     */
    private LinkedBlockingQueue<Document> documents;

    /**
     * A {@code LinkedBlockingQueue} that stores requests
     */
    private LinkedBlockingQueue<String> requests;

    /**
     * Stop?
     */
    private boolean stop = false;

    /**
     * Creates a new DocumentRequester.
     * AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA.
     */
    public DocumentRequester() {
        documents = new LinkedBlockingQueue<>();
        requests = new LinkedBlockingQueue<>();
    }

    /**
     * Requests a Document
     *
     * @param location the location of the document
     * @return the requested Document
     */
    public synchronized Document request(String location) {
        try {
            // put
            requests.offer(location);
            // take
            // log.info("Wanted {} and got {}", location, doc.title());
            return documents.take();
        } catch (InterruptedException ie) {
            log.warn("Operation was interrupted", ie);
            return null;
        }
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                String request = requests.take();
                documents.offer(Jsoup.connect(request).get());
            } catch (InterruptedException ie) {
                log.warn("Operations were interrupted", ie);
            } catch (IOException ioe) {
                log.warn("Could not connect to Wikipedia", ioe);
            }
        }
    }
}