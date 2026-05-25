package javax.jmdns.impl;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * A FIFO queue, written by Alvin Alexander (http://alvinalexander.com).
 *
 * As its name implies, this is a first-in, first-out queue.
 *
 * I developed this class for a football game I'm writing, where I want to remember
 * a limited number of plays that the "Computer Offensive Coordinator" has
 * previously called. The current need is that I don't want the computer calling
 * the same play three times in a row.
 *
 * I was going to add a `reverse` method here, but you can do that in Java with
 * `Collections.reverse(list)`, so I didn't think there was a need for it.
 *
 */
public class FifoQueueWithLimitedSize<E> implements Serializable {

    private List<E> list = new LinkedList<E>();
    private int maxSize = 3;

    public FifoQueueWithLimitedSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void put(E e) {
        list.add(e);
        if (list.size() > maxSize) list.remove(0);
    }

    /**
     * can return `null`
     */
    public E pop() {
        if (list.size() > 0) {
            E e = list.get(0);
            list.remove(0);
            return e;
        } else {
            return null; //but have a nice day
        }
    }

    /**
     * @throws
     */
    public E peekOldest() {
        return list.get(0);
    }

    public E peekLatest(){
        if(!list.isEmpty()){
            return list.get(list.size()-1);
        }
        return null;
    }

    public boolean contains(E e) {
        return list.contains(e);
    }

    /**
     * @throws
     */
    public E get(int i) {
        return list.get(i);
    }

    public List<E> getBackingList() {
        // return a copy of the list
        return new LinkedList<E>(list);
    }

    public void clear() {
        list.clear();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    // mostly needed for testing atm
    public int size() {
        return list.size();
    }

}