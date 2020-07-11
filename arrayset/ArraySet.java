package ru.ifmo.rain.akimov.arrayset;

import java.util.*;

//test21

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private SortedList<E> data;
    private Comparator<? super E> comparator;

    public ArraySet() {
        data = new SortedList<>(Collections.emptyList());
        comparator = null;
    }

    public ArraySet(Collection<? extends E> collection) {
        data = new SortedList<>(new TreeSet<>(collection));
        comparator = null;
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        Set<E> set = new TreeSet<>(comparator);
        set.addAll(collection);
        data = new SortedList<>(set);
        this.comparator = comparator;
    }

    private ArraySet(SortedList<E> sortedList, Comparator<? super E> comparator, boolean toReverse) {
        data = new SortedList<>(sortedList, sortedList.isReversed() ^ toReverse);
        this.comparator = comparator;
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Iterator<E> iterator() {
        return data.iterator();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean contains(Object o) {
        return binarySearchWrapper((E) o) >= 0;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    private void checkSize() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E first() {
        checkSize();
        return data.get(0);
    }

    @Override
    public E last() {
        checkSize();
        return data.get(data.size() - 1);
    }

    @Override
    public E lower(E e) {
        return getElementOrNull(getPosition(e) - 1);
    }

    @Override
    public E floor(E e) {
        return getElementOrNull(getPosition(e, 0, -1));
    }

    @Override
    public E ceiling(E e) {
        return getElementOrNull(getPosition(e));
    }

    @Override
    public E higher(E e) {
        return getElementOrNull(getPosition(e, 1, 0));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(data, getReverseComparator(), true);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @SuppressWarnings({"unchecked"})
    private int compare(E first, E second) {
        if (comparator == null) {
            return ((Comparable<E>) first).compareTo(second);
        } else {
            return comparator.compare(first, second);
        }
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        if (data.isEmpty()) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        } else {
            return getSubSet(fromElement, fromInclusive, toElement, toInclusive);
        }
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        if (data.isEmpty()) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        } else {
            return getSubSet(first(), true, toElement, inclusive);
        }
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        if (data.isEmpty()) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        } else {
            return getSubSet(fromElement, inclusive, last(), true);
        }
    }

    private boolean areEquals(E first, E second) {
        if (comparator == null) {
            return first.equals(second);
        } else {
            return comparator.compare(first, second) == 0;
        }
    }

    private boolean checkPosition(int pos) {
        return pos >= 0 && pos < data.size();
    }

    private boolean exists(int pos, E element) {
        return checkPosition(pos) && areEquals(data.get(pos), element);
    }

    private NavigableSet<E> getSubSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int startPos;
        if (fromElement.equals(first())) {
            startPos = 0;
        } else {
            startPos = getPosition(fromElement);
        }
        if (!fromInclusive && exists(startPos, fromElement)) {
            startPos++;
        }
        int endPos;
        if (toElement.equals(last())) {
            endPos = data.size() - 1;
        } else {
            endPos = getPosition(toElement);
        }
        if (toInclusive && exists(endPos, toElement)) {
            endPos++;
        }
        endPos = Math.max(endPos, startPos);
        return new ArraySet<>(new SortedList<>(data.subList(startPos, endPos)), comparator, false);
    }

    private int binarySearchWrapper(E element) {
        return Collections.binarySearch(data, element, comparator);
    }

    private int getPosition(E element) {
        return getPosition(element, 0, 0);
    }

    private int getPosition(E element, int addIfExists, int addIfAbsent) {
        int pos = binarySearchWrapper(element);
        if (pos >= 0) {
            return pos + addIfExists;
        } else {
            return ~pos + addIfAbsent;
        }
    }

    private E getElementOrNull(int pos) {
        if (checkPosition(pos)) {
            return data.get(pos);
        } else {
            return null;
        }
    }

    private Comparator<? super E> getReverseComparator() {
        if (comparator == null) {
            return Collections.reverseOrder();
        } else {
            return comparator.reversed();
        }
    }

    private static class SortedList<E> extends AbstractList<E> implements RandomAccess {
        private boolean reversed;
        private List<E> data;

        public SortedList(Collection<? extends E> sortedCollection) {
            data = new ArrayList<>(sortedCollection);
            reversed = false;
        }

        public SortedList(List<E> list) {
            data = list;
            reversed = false;
        }

        public SortedList(SortedList<E> list, boolean isReversed) {
            data = list.data;
            reversed = isReversed;
        }

        private int getReversedIndex(int index) {
            return size() - index - 1;
        }

        @Override
        public E get(int index) {
            return data.get(reversed ? getReversedIndex(index) : index);
        }

        @Override
        public int size() {
            return data.size();
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return reversed ? data.subList(getReversedIndex(toIndex) + 1, getReversedIndex(fromIndex) + 1) : data.subList(fromIndex, toIndex);
        }

        public boolean isReversed() {
            return reversed;
        }

    }
}
