package com.fillumina.emailrecoverer;

import java.io.File;
import java.util.Date;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class Mail extends Fragment implements Comparable<Mail> {
    private final String from, subject, id;
    private final Date date;
    private final boolean multipart;

    public Mail(File sourceFilename, File destFilename,
            String from,
            String subject,
            Date date,
            String id,
            String contentType) {
        super(sourceFilename, destFilename);
        this.date = date;
        this.from = from;
        this.subject = subject;
        this.id = id;
        this.multipart = contentType == null ?
                false : contentType.contains("multipart");
    }

    public static final String createFilename(Date date,
            String from,
            String id,
            String subject) {
        String hash = Hash.hashToString(date + from + id + subject);
        return DateExtractor.toCompactString(date) + "_" + hash;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getId() {
        return id;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "Mail{" +
                "date=" + date +
                ", from=" + from +
                ", subject=" + subject +
                ", id=" + id +
                ", multipart=" + multipart +
                ", src=" + getSourceFilename() +
                ", dst=" + getDestFilename() +
                ", fragments=" + size() + '}';
    }

    @Override
    public int compareTo(Mail o) {
        return date.compareTo(o.date);
    }
}
