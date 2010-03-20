package eea;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class FSM<A> implements Cloneable,Serializable{
	private static final long serialVersionUID = 1L;
	protected Map<A,Integer>[] transitions;
	protected boolean[] accept;
	protected int current = 0;
	protected ListSet<A> alphabet = new ListSet<A>();
	protected double fitness;
	
	public FSM(int numStates){
		transitions = new HashMap[numStates];
		for(int i = 0;i<numStates;++i) transitions[i] = new HashMap<A,Integer>();
		accept = new boolean[numStates];
	}
	public int size(){ return accept.length;}
	public Map<A,Integer>[] transitions(){return transitions;}
	public void addTransition(int state, A input, int finish){
		alphabet.add(input);
		transitions[state].put(input,finish);
	}
	public void setAccept(int state, boolean valid){accept[state] = valid;}
	public boolean check(int state){return accept[state];}
	public int current(){return current;}
	public static <A> void copy(FSM<A> src, FSM<A> dest, int begin, int end){
		for(int i = begin;i<end;++i){
			for(A k:src.transitions()[i].keySet()) dest.addTransition(i, k, src.transitions()[i].get(k));
			dest.setAccept(i, src.check(i));
		}
	}
	public String toString(){
		StringBuilder sb = new StringBuilder(20*size());//21(accepting...) + size()*14(for loops of transitions) + size()*3(for loop accepting)
		for(int i = 0;i<size();++i){
			sb.append("State ").append(i).append(":\n");
			for(A k:transitions[i].keySet()) sb.append(k).append("->").append(transitions[i].get(k)).append('\n');
		}
		sb.append("Accepting States: [");
		for(int i = 0;i<size();++i) if(check(i))sb.append(i).append(' ');
		sb.append("]\n");
		return sb.toString();
	}
	
	public boolean offer(Iterable<A> inputs){
		current = 0;
		for(A input:inputs)	current = transitions[current].get(input);
		return accept[current];
	}
	public boolean isValid(){
		Set<A> keySet = transitions[0].keySet(), temp;
		for(int i = 0;i<size();++i){
			temp = transitions[i].keySet();
			for(A k:temp)
				if(!(transitions[i].get(k)<size()&&transitions[i].get(k)>-1)) return false;
			if(!keySet.equals(temp)) return false;
		}
		return true;
	}
	public double evaluate(Map<List<A>,Boolean> labelled){
		double total = 0;
		for(List<A> l : labelled.keySet()) total+=(offer(l)!=labelled.get(l))?1:0;
		fitness = 1-(total/labelled.size());
		return fitness;
	}
	public double fitness(){return fitness;}
	public FSM<A> clone(){
		FSM<A> clone = new FSM<A>(size());
		copy(this,clone,0,size());
		return clone;
	}
	public boolean equals(Object o){
		if(o == this) return true;
		if(! (o instanceof FSM<?>)) return false;
		FSM<A> other = (FSM)o;
		if(size() == other.size() && Arrays.equals(accept, other.accept)&&alphabet.equals(other.alphabet)&&Arrays.equals(transitions, other.transitions)) return true;		
		return false;
	}
	public double accuracy(FSM<A> other, List<List<A>> allInputs){
		double num = 0,numCorrect = 0;
		for(List<A> input:allInputs){
			if(offer(input) == other.offer(input)) ++numCorrect;
			++num;
		}
		return numCorrect/num;
	}
	public static <B> FSM<B> randomFactory(int numStates, int numAccepting, Iterable<B> alphabet){
		FSM<B> ret = new FSM<B>(numStates);
		Random rand = new Random(System.nanoTime());
		List<Integer> accepting = new ArrayList<Integer>(numStates);
		for(int i = 0;i<numStates;++i){
			accepting.add(i);
			for(B a:alphabet) ret.addTransition(i, a, rand.nextInt(numStates));
		}
		Collections.shuffle(accepting,rand);
		while(numAccepting>0) ret.setAccept(accepting.get(--numAccepting), true);
		return ret;
	}
	// Generates completely random FSM with only number of states specified and alphabet.
	public static <B> FSM<B> randomFactory2(int numStates, Iterable<B> alphabet){//taking out numaccepting
		return randomFactory(numStates,new Random(System.nanoTime()).nextInt(numStates),alphabet);
//		FSM<B> ret = new FSM<B>(numStates);
//		Random rand = new Random(System.nanoTime());
//		int numAccepting = rand.nextInt(numStates);
//		List<Integer> accepting = new ArrayList<Integer>(numStates);
//		for(int i = 0;i<numStates;++i){
//			accepting.add(i);
//			for(B a:alphabet) ret.addTransition(i, a, rand.nextInt(numStates));
//		}
//		Collections.shuffle(accepting,rand);
//		while(numAccepting>0) ret.setAccept(accepting.get(--numAccepting), true);
//		return ret;
	}
	public static <B> FSM<B> simpleFactory(int numStates, Iterable<B> alph){
		FSM<B> ret = new FSM<B>(numStates);
		for(int i = 0;i<numStates;++i){
			Iterator<B> it = alph.iterator();
			B last = null;
			while(it.hasNext())
				ret.addTransition(i, last = it.next(), i);
			if(i!=numStates-1) ret.addTransition(i, last, i+1);
			if((i&1)==0)ret.setAccept(i, true);
		}
		
		return ret;
	}
	public static <B> FSM<B> countFactory(int count, Iterable<B> alph){
		FSM<B> ret = new FSM<B>(count+1);
		ret.setAccept(count, true);
		for(int i = 0;i<ret.size();++i){
			B last = null;
			for(Iterator<B> it = alph.iterator();it.hasNext();)
				ret.addTransition(i, last = it.next(), i);
			if(i!=ret.size()-1) ret.addTransition(i, last, i+1);
		}
		return ret;
	}
	public static <B> FSM<B> lessThanFactory(int count, Iterable<B> alph){
		FSM<B> ret = new FSM<B>(count+1);
		for(int i = 0;i<ret.size();++i){
			B last = null;
			for(Iterator<B> it = alph.iterator();it.hasNext();)
				ret.addTransition(i, last = it.next(), i);
			if(i!=ret.size()-1){
				ret.addTransition(i, last, i+1);
				ret.setAccept(i, true);
			}
		}
		return ret;
	}
	public static <B> FSM<B> rangeFactory(int b, int e, Iterable<B> alph){
		FSM<B> ret = new FSM<B>(e+1);
		for(int i = 0;i<ret.size();++i){
			B last = null;
			for(Iterator<B> it = alph.iterator();it.hasNext();)
				ret.addTransition(i, last = it.next(), i);
			if(i!=ret.size()-1) ret.addTransition(i, last, i+1);
			if(i>=b && i<e) ret.setAccept(i, true);
		}
		return ret;
	}
	public static <B> FSM<B> simpleFactory2(int numStates, Iterable<B> alph){
		FSM<B> ret = new FSM<B>(numStates);
		for(int i = 0;i<numStates;++i){
			Iterator<B> it = alph.iterator();
			B last = null;
			while(it.hasNext())
				ret.addTransition(i, last = it.next(), (i==0)?i:i-1);
			ret.addTransition(i, last, (i==numStates-1)?i:i+1);
			if((i&1)==0)ret.setAccept(i, true);
		}
		
		return ret;
	}
	
	public static <B> FSM<B> badHandCodedFactory(Iterable<B> alphabet){
		int numStates = 10;
		
		int[][] transitions;
		FSM<B> ret = new FSM<B>(numStates);
		List<Integer> accepting = new ArrayList<Integer>(numStates);
	    transitions = new int[numStates][2];
	    for (int i=0;i<numStates-1;++i){
	    	transitions[i][0] = i;
	    	transitions[i][1] = i+1;
	    }
	    transitions[numStates-1][0]=numStates-1;
	    transitions[numStates-1][1]=numStates-1;
		for(int i=0;i<numStates;++i){
			accepting.add(i);
			int count =0;			
			for (B a:alphabet) 
				{
       				ret.addTransition(i,a,transitions[i][count]);
       				count++;
				}
		 }
		ret.setAccept(8, true);
		ret.setAccept(9, true);
		System.err.println(ret);
		return ret;
		
		}
	
	private static List<List<Integer>> generateAllInputs(int length){
		int num = (int)Math.pow(2, length),temp;
		List<List<Integer>> all = new ArrayList<List<Integer>>(num--);
		LinkedList<Integer> one = null;
		for(;num>-1;--num){
			one = new LinkedList<Integer>();
			temp = num;
			while(one.size()<length){
				if(temp == 0) one.addFirst(0);
				else{
					one.addFirst(temp&1);
					temp>>=1;
				}
			}
			all.add(one);
		}
		return all;
	}
	public static FSM read(String filename){
		ObjectInputStream is = null;
		try{
			is = new ObjectInputStream(new FileInputStream(filename));
			return (FSM)is.readObject();
		}catch(Exception e){e.printStackTrace();return null;}
		finally{if(is!=null) try{ is.close();}catch(IOException e){	e.printStackTrace();}}
	}
	public void write(String filename){
		ObjectOutputStream os = null;
		try{
			os = new ObjectOutputStream(new FileOutputStream(filename));
			os.writeObject(this);
		}catch(IOException e){e.printStackTrace();}
		finally{if(os!=null) try{ os.close();}catch(IOException e){	e.printStackTrace();}}
	}
	public static FSM<Double> iToD(FSM<Integer> rd){
		FSM<Double> conv = new FSM<Double>(rd.size());
		ListSet<Double> al = new ListSet<Double>();
		for(int i:rd.alphabet) al.add((double)i);
		for(int i = 0;i<rd.transitions.length;++i){
			for(int k:rd.transitions[i].keySet())
				conv.addTransition(i, (double)k, rd.transitions[i].get(k));
			conv.setAccept(i, rd.check(i));
		}
		return conv;
	}
	public static void main(String[] args){
		List<Integer> alph = Arrays.asList(0,1);
		FSM<Integer> bad = badHandCodedFactory(alph);
		int accept = 0;
		for(List<Integer> l:generateAllInputs(10))
			if(bad.offer(l)) ++accept;
		System.out.println(accept);
		System.exit(0);
		FSM<Integer> range28 = rangeFactory(2,8,alph);
		FSM<Integer> lt5 = lessThanFactory(5,alph);
		System.out.println(lt5);
		System.out.println(lt5.offer(Arrays.asList(0,0)));
		System.out.println(lt5.offer(Arrays.asList(0,1)));
		System.out.println(lt5.offer(Arrays.asList(0,1,1)));
		System.out.println(lt5.offer(Arrays.asList(0,1,0,1,0,0,1,0,1)));
		System.out.println(lt5.offer(Arrays.asList(0,1,0,1,1,0,0,1,0,1)));
		System.exit(0);
		System.out.println(range28);
		System.out.println(range28.offer(Arrays.asList(0,1)));
		System.out.println(range28.offer(Arrays.asList(0,1,1)));
		System.out.println(range28.offer(Arrays.asList(0,1,0,1)));
		System.out.println(range28.offer(Arrays.asList(0,1,0,1,1,0,1,0,0,1,0,1,0,1)));
		System.out.println(range28.offer(Arrays.asList(0,1,1,1,1,1,1,1,1)));
		System.out.println(range28.offer(Arrays.asList(0,1,1,1,1,1,1,1,1,1)));
		System.exit(0);
		for(int i = 0;i<12;++i){
			FSM<Integer> counter = countFactory(i, alph);
			FSM<Integer> counter2 = countFactory(i+1, alph);
			//System.out.println(counter);
			int accept2 = 0;
			for(List<Integer> l:generateAllInputs(10))
				if(counter.offer(l) != counter2.offer(l)) ++accept2;
			System.out.println(i+"->"+(i+1)+": "+accept2);
			
		}
		System.exit(0);
		FSM<Integer> rd = FSM.read("approx.fsm");
		System.out.println(rd);
		System.exit(0);
		FSM<Double> out = iToD(rd);
		FSM<Double> conv = new FSM<Double>(rd.size());
		System.out.println(rd);
		System.out.println(out);
		System.exit(0);
		ListSet<Double> al = new ListSet<Double>();
		for(int i:rd.alphabet) al.add((double)i);
		for(int i = 0;i<rd.transitions.length;++i){
			for(int k:rd.transitions[i].keySet())
				conv.addTransition(i, (double)k, rd.transitions[i].get(k));
			conv.setAccept(i, rd.check(i));
		}
		System.out.println(conv);
		
		System.exit(0);
		
		
		FSM<Integer> f2 = simpleFactory2(4,alph);
		System.out.println(f2);
		
		
		//badHandCodedFactory(alph);
		System.exit(0);
		FSM<Integer> fsm = randomFactory(10,2,alph);
		List<Integer> input = Arrays.asList(0,1,1,0,1,0);
		System.out.println(fsm);
		System.out.println(fsm.offer(input));
		System.out.println(fsm.current());
		FSM<Integer> fsm1 = FSM.randomFactory(10,2,alph);
		FSM<Integer> fsm2 = FSM.randomFactory(10,2,alph);
		EvoFSM.breed(fsm1, fsm2);
	}
	

}
