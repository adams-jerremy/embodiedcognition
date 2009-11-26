import java.util.Arrays;
import java.util.List;
import java.util.Random;


interface FSMCrossover{
	<A> FSM<A> cross(FSM<A> p1, FSM<A> p2);
}

class FSMCrossOnePt implements FSMCrossover{
	public static final FSMCrossOnePt only = new FSMCrossOnePt();
	private static final Random rand = new Random(System.nanoTime());
	private FSMCrossOnePt(){}
	public <A> FSM<A> cross(FSM<A> p1, FSM<A> p2){
		int crossPt = rand.nextInt(p1.size());
		FSM<A> ret = new FSM<A>(p1.size());
		FSM.copy(p1, ret, 0, crossPt);
		FSM.copy(p2, ret, crossPt, p2.size());
			
		System.out.println("Parent 1: "+p1+"\nParent2: "+p2+"\nCross Point: "+crossPt+"\n Child: "+ret);
		return ret;
	}
	
}
class FSMCrossTwoPt implements FSMCrossover{
	public <A> FSM<A> cross(FSM<A> p1, FSM<A> p2){
		return null;
	}
}
class FSMCrossUniform implements FSMCrossover{
	public <A> FSM<A> cross(FSM<A> p1, FSM<A> p2){
		return null;
	}
}
class FSMCrossNone implements FSMCrossover{
	public <A> FSM<A> cross(FSM<A> p1, FSM<A> p2){
		return null;
	}
}