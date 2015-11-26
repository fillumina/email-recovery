package com.fillumina.emailrecoverer;

import java.util.Iterator;

/**
 * Encapsulates the mechanism of a read only iterator.
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public abstract class AbstractReadOnlyIterator<T>
        implements Iterator<T>, Iterable<T> {

    private T nextElement;
    private T previousElement;

    public AbstractReadOnlyIterator() {
    }

    public AbstractReadOnlyIterator(final T startElement) {
        this.nextElement = startElement;
    }

    /** Remember, this method can be called only once for the object! */
    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        readAhead();
        return nextElement != null;
    }

    @Override
    public T next() {
        readAhead();
        previousElement = nextElement;
        nextElement = null;
        return previousElement;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    private void readAhead() {
        if (nextElement == null) {
            nextElement = getNext(previousElement);
        }
    }

    /** @return null if no other elements are available */
    protected abstract T getNext(T current);
}
