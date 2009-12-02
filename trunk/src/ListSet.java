import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

//
public class ListSet<A> implements List<A>, Set<A>{
	protected final List<A> l;
	protected final Set<A> s;
	protected final Random rand = new Random(System.nanoTime());
	public ListSet(){
		l = new ArrayList<A>();
		s = new HashSet<A>();}
	public ListSet(Set<A> set){
		l = new ArrayList<A>(set.size());
		s = set;
		l.addAll(set);
	}
	public ListSet(Iterable<A> i){
		this();
		for(A e:i) add(e);
	}
	public boolean add(A e){
		if (s.contains(e)) return false;
		l.add(e);
		return s.add(e);
	}
	public void add(int index, A e){ addIndex(index,e);}
	private boolean addIndex(int index, A e){
		if(!s.contains(e)){
			s.add(e);
			l.add(index,e);
			return true;
		}
		return false;
	}
	public boolean addAll(Collection<? extends A> c){
		boolean changed = false;
		for(A e:c) changed|=add(e);
		return changed;
	}
	public boolean addAll(int index, Collection<? extends A> c){
		boolean changed = false;
		for(A e:c) changed|=addIndex(index,e);
		return changed;
	}
	public void clear(){
		l.clear();
		s.clear();
	}
	public boolean contains(Object o){ return s.contains(o);}
	public boolean containsAll(Collection<?> c){ return s.containsAll(c);}
	public A get(int index){ return l.get(index);}
	public int indexOf(Object o){ return l.indexOf(o);}
	public boolean isEmpty(){ return s.isEmpty();}
	public Iterator<A> iterator(){ return listIterator();}
	public int lastIndexOf(Object o){ return l.lastIndexOf(o);}
	public ListIterator<A> listIterator(){ return listIterator(0); }
	public ListIterator<A> listIterator(final int index){
		return new ListIterator<A>(){
			ListIterator<A> li = l.listIterator(index);
			A last;
			public void add(A e){ if(s.add(e)) li.add(e);}
			public boolean hasNext(){ return li.hasNext(); }
			public boolean hasPrevious(){ return li.hasPrevious();}
			public A next(){ return (last = li.next());	}
			public int nextIndex(){ return li.nextIndex();}
			public A previous(){ return (last=li.previous());}
			public int previousIndex(){return li.previousIndex();}
			public void remove(){
				li.remove();
				s.remove(last);
				last = null;
			}
			public void set(A e){				
				remove();
				add(e);
			}
		};
	}
	public boolean remove(Object o){
		l.remove(o);
		return s.remove(o);
	}
	public A remove(int index){
		s.remove(get(index));
		return l.remove(index);
	}
	public boolean removeAll(Collection<?> c){
		boolean ret = false;
		for(Object e:c) ret|=remove(e);
		return ret;
	}
	public boolean retainAll(Collection<?> c){
		if(s.retainAll(c))
			return l.retainAll(s);
		return false;
	}
	public A set(int index, A e){
		ListIterator<A> li = listIterator(index);
		A ret = li.next();
		li.set(e);
		return ret;
	}
	public A choice(){return get(rand.nextInt(size()));}
	public int size(){ return l.size();}
	public List<A> subList(int fromIndex, int toIndex){
	        return null;
	}
	public Object[] toArray(){ return l.toArray();}
	public <T> T[] toArray(T[] a){ return l.toArray(a);}
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof ListSet)) return false;
		ListSet other = (ListSet)o;
		return l.equals(other.l);
	}
}
