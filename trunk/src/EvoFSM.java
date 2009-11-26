import java.util.Iterator;
import java.util.Random;


public class EvoFSM {
	private static double MUTATION = .1;
	private static FSMCrossover TYPE = FSMCrossOnePt.only;
	private static final Random rand = new Random(System.nanoTime());
	public static <A> void mutate(FSM<A> fsm){
		if(rand.nextDouble()<MUTATION)
			fsm.transitions[rand.nextInt(fsm.size())].put(fsm.alphabet.get(rand.nextInt(fsm.alphabet.size())),rand.nextInt(fsm.size()));
	}
	public static <A> FSM<A> breed(FSM<A> mate1, FSM<A> mate2){
		return TYPE.cross(mate1, mate2);
	}
	public static void setMutationProbability(double m){MUTATION = m;}
	public static void setCrossoverType(FSMCrossover t){TYPE = t;}
	
	
}
