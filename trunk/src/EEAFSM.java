import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

public class EEAFSM{
	private static int NUM_STATES = 10, NUM_ACCEPTING = 2, INPUT_LENGTH = 10, 
		POPULATION_SIZE=10, NUM_POPS = 2, ESTIMATION_ITERATIONS = (int)((3.0/8.0)*NUM_POPS*POPULATION_SIZE),
		MUTATION_GENERATIONS = 50;
	private static ExampleGenerator<Integer> EXAMPLE_GENERATOR = new ActiveExample();
	private static final ListSet<Integer> ALPHABET = new ListSet<Integer>(Arrays.asList(0,1));
	private static final FSM<Integer> TARGET = FSM.randomFactory(NUM_STATES, NUM_ACCEPTING, ALPHABET);
	private static final Map<List<Integer>,Boolean> labelled = new HashMap<List<Integer>,Boolean>();
	private static final List<List<FSM<Integer>>> populations = new ArrayList<List<FSM<Integer>>>(NUM_POPS);
	private static final Random rand = new Random(System.nanoTime());
	private static final List<List<Integer>> ALL_INPUTS = generateAllInputs(INPUT_LENGTH);
	private static final Comparator<FSM<Integer>> fitnessComp = new Comparator<FSM<Integer>>(){
		public int compare(FSM<Integer> o1, FSM<Integer> o2){return o1.fitness()>o2.fitness()?-1:o2.fitness()>o1.fitness()?1:0;}};
	private static final Comparator<Pair<List<Integer>,Double>> sentComp = new Comparator<Pair<List<Integer>,Double>>(){
		public int compare(Pair<List<Integer>,Double> o1, Pair<List<Integer>,Double> o2){return o1.second>o2.second?-1:o2.second>o1.second?1:0; }};
	private static final class RandomExample implements ExampleGenerator<Integer>{
			public List<Integer> generateExample(){return randomSentence();}}
	private static final class InOrderExample implements ExampleGenerator<Integer>{
			Iterator<List<Integer>> it = generateAllInputs(INPUT_LENGTH).iterator();
			public List<Integer> generateExample(){
				if(!it.hasNext()) it = generateAllInputs(INPUT_LENGTH).iterator();
				return it.next();}}
	private static final class ActiveExample implements ExampleGenerator<Integer>{
		public List<Integer> generateExample(){
			List<Pair<List<Integer>,Double>> sentencePop = new LinkedList<Pair<List<Integer>,Double>>();
			for(int i = 0;i<POPULATION_SIZE*NUM_POPS;++i) sentencePop.add(new Pair<List<Integer>,Double>(randomSentence(),0.0));
			for(int gen = 0; gen<MUTATION_GENERATIONS;++gen){
				evaluateSentences(sentencePop);
				Collections.sort(sentencePop,sentComp);
				for(int i =0;i<ESTIMATION_ITERATIONS;++i){
					int fit = rand.nextInt(sentencePop.size()),unfit = rand.nextInt(sentencePop.size());
					if(fit>unfit){ int temp = fit; fit = unfit; unfit = (temp==0)?sentencePop.size()-1:temp;}
					sentencePop.get(unfit).first = mutateSentence(sentencePop.get(fit).first);
				}
			}
			evaluateSentences(sentencePop);
			double max = -Double.MAX_VALUE;
			List<Integer> argmax = null;
			for(Pair<List<Integer>,Double> p:sentencePop)
				if(p.second>max){max=p.second; argmax = p.first;}
			return argmax;
		}}

	private static void handleArgs(String[] args){
		String s;
		for(Iterator<String> i = Arrays.asList(args).iterator();i.hasNext();){
			s = i.next();
			switch(s.charAt(0)){
			case '-':
				switch(s.charAt(1)){
				case 't': setType(i.next().toLowerCase()); break;
				case 'g': setGenerations(i.next()); break;
				case 'p': setPopulationSize(i.next()); break;
				case 's': setSize(i.next()); break;
				default: System.err.println("Unrecognized Option:"+s); break;
				}
				break;
			default: break;
			}
		}
	}
	private static void setType(String s){
		switch(s.charAt(0)){
		case 'r': EXAMPLE_GENERATOR = new RandomExample(); break;
		case 'a': EXAMPLE_GENERATOR = new ActiveExample(); break;
		case 'i': EXAMPLE_GENERATOR = new InOrderExample(); break;
		default: System.err.println("Unrecognized Type Requested:"+s); break;
		}
	}
	private static void setGenerations(String s){
		try{MUTATION_GENERATIONS = Integer.parseInt(s);
		}catch(NumberFormatException e){ System.err.println("Imparseable number of generations: "+s);}
	}
	private static void setPopulationSize(String s){
		try{POPULATION_SIZE = Integer.parseInt(s);
		}catch(NumberFormatException e){ System.err.println("Imparseable number for population: "+s);}
	}
	private static void setSize(String s){
		try{NUM_STATES = Integer.parseInt(s);
		}catch(NumberFormatException e){ System.err.println("Imparseable size: "+s);}
	}
	public static void main(String[] args){
		handleArgs(args);
		init();
		for(int i = 0;i<1000;++i){
			if(i%50==0){
				System.err.println("Iteration "+i);
				for(List<FSM<Integer>> pop:populations){
					double accuracy = pop.get(0).accuracy(TARGET,ALL_INPUTS);
					System.err.println("Max in population fitness: "+pop.get(0).fitness());
					System.err.println("Max in population accuracy: "+accuracy);
					if(accuracy == 1.0){System.err.println("Solution found! Exiting"); System.exit(0);}
				}
			}
			est();
			List<Integer> nextTest = EXAMPLE_GENERATOR.generateExample();
			labelled.put(nextTest,TARGET.offer(nextTest));
		}
	}
	private static void est(){
		if(populations.size()==0){
			for(int i =0;i<NUM_POPS;++i){
				populations.add(new ArrayList<FSM<Integer>>(POPULATION_SIZE));
				for(int j =0;j<POPULATION_SIZE;++j) populations.get(i).add(FSM.randomFactory(NUM_STATES, NUM_ACCEPTING, ALPHABET));
			}
		}else{
			for(List<FSM<Integer>> pop:populations){
				ListIterator<FSM<Integer>> li = pop.listIterator(2);
				while(li.hasNext()){ li.next(); li.set(FSM.randomFactory(NUM_STATES, NUM_ACCEPTING, ALPHABET));}
			}
		}
		for(List<FSM<Integer>> pop: populations){
			for(int gen = 0;gen<MUTATION_GENERATIONS;++gen){
				for(FSM<Integer> fsm:pop) fsm.evaluate(labelled);
				Collections.sort(pop,fitnessComp);
				for(int i =0;i<ESTIMATION_ITERATIONS;++i){
					int fit = rand.nextInt(pop.size()),unfit = rand.nextInt(pop.size());
					if(fit>unfit){ int temp = fit; fit = unfit; unfit = (temp==0)?pop.size()-1:temp;}//TODO: ??? Is this okay?
					pop.set(unfit, EvoFSM.mutate(pop.get(fit)));//TODO: Change mutation to also mutate accepting states?
				}
			}
			for(FSM<Integer> fsm:pop) fsm.evaluate(labelled);
			Collections.sort(pop,fitnessComp);
		}
	}
//	private static List<Integer> exp(){
//		List<Pair<List<Integer>,Double>> sentencePop = new LinkedList<Pair<List<Integer>,Double>>();
//		for(int i = 0;i<POPULATION_SIZE*NUM_POPS;++i) sentencePop.add(new Pair<List<Integer>,Double>(randomSentence(),0.0));
//		for(int gen = 0; gen<MUTATION_GENERATIONS;++gen){
//			evaluateSentences(sentencePop);
//			Collections.sort(sentencePop,sentComp);
//			for(int i =0;i<ESTIMATION_ITERATIONS;++i){
//				int fit = rand.nextInt(sentencePop.size()),unfit = rand.nextInt(sentencePop.size());
//				if(fit>unfit){ int temp = fit; fit = unfit; unfit = (temp==0)?sentencePop.size()-1:temp;}
//				sentencePop.get(unfit).first = mutateSentence(sentencePop.get(fit).first);
//			}
//		}
//		evaluateSentences(sentencePop);
//		double max = -Double.MAX_VALUE;
//		List<Integer> argmax = null;
//		for(Pair<List<Integer>,Double> p:sentencePop)
//			if(p.second>max){max=p.second; argmax = p.first;}
//		return argmax;
//	}
	
	private static List<Integer> mutateSentence(List<Integer> sent){
		int mutLoc = rand.nextInt(sent.size());
		List<Integer> ret = new ArrayList<Integer>(sent.size());
		for(int i:sent) ret.add(i);
		ret.set(mutLoc, ret.get(mutLoc)==1?0:1);
		return ret;
	}
	private static void evaluateSentences(List<Pair<List<Integer>, Double>> sentencePop){
		for(Pair<List<Integer>,Double> p : sentencePop){
			for(List<FSM<Integer>> pop:populations)
				//for(FSM<Integer> fsm:pop) p.second+=fsm.offer(p.first)?1:0; Only offer top two to Exploration?
				p.second+=(pop.get(0).offer(p.first)?1:0) + (pop.get(1).offer(p.first)?1:0) ;
			p.second = 1.0 - 2.0*Math.abs(.5-p.second/(2*NUM_POPS));
		}
	}
	private static void init(){
		List<Integer> firstExample = randomSentence();
		labelled.put(firstExample, TARGET.offer(firstExample));
	}
	private static List<Integer> randomSentence(){
		List<Integer> ret = new ArrayList<Integer>(INPUT_LENGTH);
		for(int i = 0;i<INPUT_LENGTH;++i) ret.add(ALPHABET.choice());
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
	
}
class Pair<T,E>{T first;E second;public Pair(T f, E s){first = f; second = s;}}
interface ExampleGenerator<T>{ List<T> generateExample();}

