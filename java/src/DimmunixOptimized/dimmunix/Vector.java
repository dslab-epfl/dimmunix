/*
     Created by Horatiu Jula, George Candea, Daniel Tralamazza, Cristian Zamfir
     Copyright (C) 2009 EPFL (Ecole Polytechnique Federale de Lausanne)

     This file is part of Dimmunix.

     Dimmunix is free software: you can redistribute it and/or modify it
     under the terms of the GNU General Public License as published by the
     Free Software Foundation, either version 3 of the License, or (at
     your option) any later version.

     Dimmunix is distributed in the hope that it will be useful, but
     WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     General Public License for more details.

     You should have received a copy of the GNU General Public
     License along with Dimmunix. If not, see http://www.gnu.org/licenses/.

     EPFL
     Dependable Systems Lab (DSLAB)
     Room 330, Station 14
     1015 Lausanne
     Switzerland
*/

package dimmunix;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

public class Vector<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

	private static final long serialVersionUID = -6395873749723108850L;
	
	protected Object[] elementData;
	protected int elementCount;
	protected int capacityIncrement;

	public Vector(int initialCapacity, int capacityIncrement) {
		super();
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Capacity: "+ initialCapacity);
		this.elementData = new Object[initialCapacity];
		this.capacityIncrement = capacityIncrement;
	}

	public Vector(int initialCapacity) {
		this(initialCapacity, 0);
	}

	public Vector() {
		this(10);
	}

	public Vector(Collection<? extends E> c) {
		elementCount = c.size();
		// 10% for growth
		elementData = new Object[(int)Math.min((elementCount*110L)/100,Integer.MAX_VALUE)]; 
		c.toArray(elementData);
	}
	
	public Vector(Object[] elementData) {
		this.elementData = elementData;
		this.elementCount = elementData.length;
	}
	
	public void copyInto(Object[] anArray) {
		System.arraycopy(elementData, 0, anArray, 0, elementCount);
	}

	public void copyInto(Vector<E> v) {
		v.setSize(elementCount);
		System.arraycopy(elementData, 0, v.elementData, 0, elementCount);
	}

	public void trimToSize() {
		int oldCapacity = elementData.length;
		if (elementCount < oldCapacity) {
			Object oldData[] = elementData;
			elementData = new Object[elementCount];
			System.arraycopy(oldData, 0, elementData, 0, elementCount);
		}
	}

	public void ensureCapacity(int minCapacity) {
		ensureCapacityHelper(minCapacity);
	}

	private void ensureCapacityHelper(int minCapacity) {
		int oldCapacity = elementData.length;
		if (minCapacity > oldCapacity) {
			Object[] oldData = elementData;
			int newCapacity = (capacityIncrement > 0) ?
					(oldCapacity + capacityIncrement) : (oldCapacity * 2);
					if (newCapacity < minCapacity) {
						newCapacity = minCapacity;
					}
					elementData = new Object[newCapacity];
					System.arraycopy(oldData, 0, elementData, 0, elementCount);
		}
	}

	public void setSize(int newSize) {
		if (newSize > elementCount) {
			ensureCapacityHelper(newSize);
		} else {
			for (int i = newSize ; i < elementCount ; i++) {
				elementData[i] = null;
			}
		}
		elementCount = newSize;
	}

	public int capacity() {
		return elementData.length;
	}

	public int size() {
		return elementCount;
	}

	public boolean isEmpty() {
		return elementCount == 0;
	}

	public Enumeration<E> elements() {
		return new Enumeration<E>() {
			int count = 0;

			public boolean hasMoreElements() {
				return count < elementCount;
			}

			public E nextElement() {
				if (count < elementCount) {
					return (E)elementData[count++];
				}
				throw new NoSuchElementException("Vector Enumeration");
			}
		};
	}

	public boolean contains(Object elem) {
		return indexOf(elem, 0) >= 0;
	}

	public E find(Object elem) {
		for (int i = 0 ; i < elementCount ; i++)
			if (elementData[i].equals(elem))
				return (E)elementData[i];
		return null;
	}

	public int indexOf(Object elem) {
		return indexOf(elem, 0);
	}

	public int indexOf(Object elem, int index) {
		if (elem == null) {
			for (int i = index ; i < elementCount ; i++)
				if (elementData[i]==null)
					return i;
		} else {
			for (int i = index ; i < elementCount ; i++)
				if (elem.equals(elementData[i]))
					return i;
		}
		return -1;
	}
	
	public int lastIndexOf(Object elem) {
		return lastIndexOf(elem, elementCount-1);
	}

	public int lastIndexOf(Object elem, int index) {
		if (index >= elementCount)
			throw new IndexOutOfBoundsException(index + " >= "+ elementCount);

		if (elem == null) {
			for (int i = index; i >= 0; i--)
				if (elementData[i]==null)
					return i;
		} else {
			for (int i = index; i >= 0; i--)
				if (elem.equals(elementData[i]))
					return i;
		}
		return -1;
	}

	public E elementAt(int index) {
		if (index >= elementCount) {
			throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
		}

		return (E)elementData[index];
	}

	public E firstElement() {
		if (elementCount == 0) {
			throw new NoSuchElementException();
		}
		return (E)elementData[0];
	}

	public E lastElement() {
		if (elementCount == 0) {
			throw new NoSuchElementException();
		}
		return (E)elementData[elementCount - 1];
	}

	public void setElementAt(E obj, int index) {
		if (index >= elementCount) {
			throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
		}
		elementData[index] = obj;
	}

	public void removeElementAt(int index) {
		if (index >= elementCount) {
			throw new ArrayIndexOutOfBoundsException(index + " >= " + elementCount);
		}
		else if (index < 0) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		int j = elementCount - index - 1;
		if (j > 0) {
			System.arraycopy(elementData, index + 1, elementData, index, j);
		}
		elementCount--;
		elementData[elementCount] = null; /* to let gc do its work */
	}

	public void insertElementAt(E obj, int index) {
		if (index > elementCount) {
			throw new ArrayIndexOutOfBoundsException(index
					+ " > " + elementCount);
		}
		ensureCapacityHelper(elementCount + 1);
		System.arraycopy(elementData, index, elementData, index + 1, elementCount - index);
		elementData[index] = obj;
		elementCount++;
	}

	public void addElement(E obj) {
		ensureCapacityHelper(elementCount + 1);
		elementData[elementCount++] = obj;
	}

	public boolean removeElement(Object obj) {
		int i = indexOf(obj);
		if (i >= 0) {
			removeElementAt(i);
			return true;
		}
		return false;
	}

	public void removeAllElements() {
		// Let gc do its work
		for (int i = 0; i < elementCount; i++)
			elementData[i] = null;

		elementCount = 0;
	}

	public Object clone() {
		try {
			Vector<E> v = (Vector<E>) super.clone();
			v.elementData = new Object[elementCount];
			System.arraycopy(elementData, 0, v.elementData, 0, elementCount);
			return v;
		} catch (CloneNotSupportedException e) { 
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
	}

	public Object[] toArray() {
		Object[] result = new Object[elementCount];
		System.arraycopy(elementData, 0, result, 0, elementCount);
		return result;
	}

	public <T> T[] toArray(T[] a) {
		if (a.length < elementCount)
			a = (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), elementCount);

		System.arraycopy(elementData, 0, a, 0, elementCount);

		if (a.length > elementCount)
			a[elementCount] = null;

		return a;
	}

	// Positional Access Operations

	public E get(int index) {
		if (index >= elementCount)
			throw new ArrayIndexOutOfBoundsException(index);

		return (E)elementData[index];
	}

	public E set(int index, E element) {
		if (index >= elementCount)
			throw new ArrayIndexOutOfBoundsException(index);

		Object oldValue = elementData[index];
		elementData[index] = element;
		return (E)oldValue;
	}

	public boolean add(E o) {
		ensureCapacityHelper(elementCount + 1);
		elementData[elementCount++] = o;
		return true;
	}

	public boolean remove(Object o) {
		return removeElement(o);
	}

	public void add(int index, E element) {
		insertElementAt(element, index);
	}

	public E remove(int index) {
		if (index >= elementCount)
			throw new ArrayIndexOutOfBoundsException(index);
		Object oldValue = elementData[index];

		int numMoved = elementCount - index - 1;
		if (numMoved > 0)
			System.arraycopy(elementData, index+1, elementData, index, numMoved);
		elementData[--elementCount] = null; // Let gc do its work

		return (E)oldValue;
	}

	public void clear() {
		removeAllElements();
	}

	// Bulk Operations

	public boolean containsAll(Collection<?> c) {
		return super.containsAll(c);
	}

	public boolean addAll(Collection<? extends E> c) {
		Object[] a = c.toArray();
		int numNew = a.length;
		ensureCapacityHelper(elementCount + numNew);
		System.arraycopy(a, 0, elementData, elementCount, numNew);
		elementCount += numNew;
		return numNew != 0;
	}

	public boolean removeAll(Collection<?> c) {
		return super.removeAll(c);
	}

	public boolean retainAll(Collection<?> c)  {
		return super.retainAll(c);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		if (index < 0 || index > elementCount)
			throw new ArrayIndexOutOfBoundsException(index);

		Object[] a = c.toArray();
		int numNew = a.length;
		ensureCapacityHelper(elementCount + numNew);

		int numMoved = elementCount - index;
		if (numMoved > 0)
			System.arraycopy(elementData, index, elementData, index + numNew, numMoved);

		System.arraycopy(a, 0, elementData, index, numNew);
		elementCount += numNew;
		return numNew != 0;
	}

	public boolean equals(Object o) {
		if (o == this)
		    return true;
		if (o == null || !(o instanceof Vector))
		    return false;

		Vector<E> v = (Vector<E>)o;
		
		if (elementCount != v.elementCount)
			return false;
		
		for (int i = 0; i < elementCount; i++) {
		    if (!(elementAt(i) == null ? v.elementAt(i) == null : elementAt(i).equals(v.elementAt(i))))
		    	return false;
		}
		return true;
	}

	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < elementCount; i++) {
		    E obj = elementAt(i);
		    hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
		}
		return hashCode;
	}

	public String toString() {
		return super.toString();
	}

	protected void removeRange(int fromIndex, int toIndex) {
		int numMoved = elementCount - toIndex;
		System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);

		// Let gc do its work
		int newElementCount = elementCount - (toIndex-fromIndex);
		while (elementCount != newElementCount)
			elementData[--elementCount] = null;
	}
	
	public void swap(int i, int j) {
		Object x = elementData[i];
		elementData[i] = elementData[j];
		elementData[j] = x;
	}
	
	public E removeFast(int i) {
		swap(i, elementCount- 1);
		return remove(elementCount- 1);
	}
	
	public Vector<E> cloneVector() {
		Vector<E> v = new Vector<E>(elementCount);
		v.elementCount = elementCount;
		System.arraycopy(elementData, 0, v.elementData, 0, elementCount);
		return v;
	}
	
	public E remove() {
		return remove(elementCount- 1);
	}
	
	public boolean containsRef(E x) {
		for (int i = 0; i < elementCount; i++) {
			if (elementAt(i) == x)
				return true;
		}
		return false;
	}
	
	public boolean intersects(Vector<E> v) {
		if (v == null) {
			return false;
		}
		
		for (int i = 0; i < elementCount; i++) {
			if (v.contains(this.elementAt(i))) {
				return true;
			}
		}
		
		return false;
	}
}


