import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class FSM<A>{
	protected Map<A,Integer>[] transitions;
	protected boolean[] accept;
	protected int current = 0;
	protected ListSet<A> alphabet = new ListSet<A>();
	
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
		StringBuilder sb = new StringBuilder();
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
	
	public static void main(String[] args){
		List<Integer> alph = Arrays.asList(0,1);
		FSM<Integer> fsm = randomFactory(10,2,alph);
		List<Integer> input = Arrays.asList(0,1,1,0,1,0);
//		System.out.println(fsm);
//		System.out.println(fsm.offer(input));
//		System.out.println(fsm.current());
		
		FSM<Integer> fsm1 = FSM.randomFactory(10,2,alph);
		FSM<Integer> fsm2 = FSM.randomFactory(10,2,alph);
		EvoFSM.breed(fsm1, fsm2);
		
	}
	

}
