package org.EEA;
import java.util.Random;

// This most probably represents the estimation phase where FSM models are evolved. 
public class EvoFSM {
	private static double MUTATION = 1.1;
	private static FSMCrossover TYPE = FSMCrossOnePt.only;
	private static final Random rand = new Random(System.nanoTime());
	public static <A> void mutate2(FSM<A> fsm){//in place
		if(rand.nextDouble()<MUTATION)
			fsm.transitions[rand.nextInt(fsm.size())].put(fsm.alphabet.choice(),rand.nextInt(fsm.size()));
	}
	public static <A> FSM<A> mutate(FSM<A> fsm){
		FSM<A> clone = fsm.clone();
		clone.transitions[rand.nextInt(fsm.size())].put(fsm.alphabet.choice(), rand.nextInt(fsm.size()));
		return clone;
	}
	public static <A> FSM<A> breed(FSM<A> mate1, FSM<A> mate2){
		return TYPE.cross(mate1, mate2);
	}
	public static void setMutationProbability(double m){MUTATION = m;}
	public static void setCrossoverType(FSMCrossover t){TYPE = t;}
	
	
}
