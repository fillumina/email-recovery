package com.fillumina.emailrecoverer;

import java.io.File;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Objects;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class Fragment extends AbstractCollection<Fragment> {
    private final File sourceFilename;
    private final File destFilename;
    private Fragment next;

    public Fragment(File sourceFilename, File destFilename) {
        this.sourceFilename = sourceFilename;
        this.destFilename = destFilename;
    }

    public void chain(Fragment next) {
        this.next = next;
    }

    public File getSourceFilename() {
        return sourceFilename;
    }

    public File getDestFilename() {
        return destFilename;
    }

    @Override
    public int size() {
        if (next == null) {
            return 0;
        }
        int counter = 0;
        for (Fragment f : this) {
            counter++;
        }
        return counter;
    }

    @Override
    public Iterator<Fragment> iterator() {
        return new Iterator<Fragment>() {
            Fragment current = Fragment.this;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public Fragment next() {
                Fragment result = current;
                current = current.next;
                return result;
            }
        };
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.destFilename);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Fragment other = (Fragment) obj;
        return Objects.equals(this.destFilename, other.destFilename);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "sourceFilename=" + sourceFilename.getAbsolutePath() +
                ", destFilename=" + destFilename.getAbsolutePath() +
                ", fragments= " + size() +
                '}';
    }
}
