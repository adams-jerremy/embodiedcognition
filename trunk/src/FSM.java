import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

//
public class FSM<A> implements Cloneable{
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
		double numCorrect = 0;
		List<List<A>> wrong = new ArrayList<List<A>>();
		for(List<A> input:allInputs){
			if(offer(input) == other.offer(input)) ++numCorrect;
			else wrong.add(input);
		}
		if(numCorrect/allInputs.size() > .99) System.err.println("WRONG: "+wrong);
		return numCorrect/allInputs.size();
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
	// Generate Bad FSM
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
	// Generates completely random FSM with only number of states specified and alphabet.
	public static <B> FSM<B> randomFactory2(int numStates, Iterable<B> alphabet){//taking out numaccepting
		FSM<B> ret = new FSM<B>(numStates);
		Random rand = new Random(System.nanoTime());
		int numAccepting = rand.nextInt(numStates);
		List<Integer> accepting = new ArrayList<Integer>(numStates);
		for(int i = 0;i<numStates;++i){
			accepting.add(i);
			for(B a:alphabet) ret.addTransition(i, a, rand.nextInt(numStates));
		}
		Collections.shuffle(accepting,rand);
		while(numAccepting>0) ret.setAccept(accepting.get(--numAccepting), true);
		return ret;
	}
	
	public static void main(String[] args){
		List<Integer> alph = Arrays.asList(0,1);
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
