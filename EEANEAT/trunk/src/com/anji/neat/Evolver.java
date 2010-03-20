/*
 * Copyright (C) 2004 Derek James and Philip Tucker
 * 
 * This file is part of ANJI (Another NEAT Java Implementation).
 * 
 * ANJI is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * 
 * created by Philip Tucker on Feb 16, 2003
 */
package com.anji.neat;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;
import org.jgap.Genotype;
import org.jgap.event.GeneticEvent;

import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.LogEventListener;
import com.anji.integration.PersistenceEventListener;
import com.anji.integration.PresentationEventListener;
import com.anji.integration.TargetFitnessFunction;
import com.anji.integration.TranscriberException;
import com.anji.persistence.Persistence;
import com.anji.run.Run;
import com.anji.util.Configurable;
import com.anji.util.Properties;
import com.anji.util.Reset;

import eea.EEAFSM;
import eea.ExampleGenerator;
import eea.FSM;
import eea.ListSet;
import eea.Pair;

/**
 * Configures and performs an ANJI evolutionary run.
 * 
 * @author Philip Tucker
 */
public class Evolver implements Configurable {

private static Logger logger = Logger.getLogger( Evolver.class );

/**
 * properties key, # generations in run
 */
public static final String NUM_GENERATIONS_KEY = "num.generations";

/**
 * properties key, fitness function class
 */
public static final String FITNESS_FUNCTION_CLASS_KEY = "fitness_function";

private static final String FITNESS_THRESHOLD_KEY = "fitness.threshold";

private static final String RESET_KEY = "run.reset";

/**
 * properties key, target fitness value - after reaching this run will halt
 */
public static final String FITNESS_TARGET_KEY = "fitness.target";

private NeatConfiguration config = null;

private Chromosome champ = null;

private Genotype genotype = null;

private int numEvolutions = 0;

private double targetFitness = 0.0d;

private double thresholdFitness = 0.0d;

private int maxFitness = 0;

private Persistence db = null;


private ActivatorTranscriber ac;
/**
 * ctor; must call <code>init()</code> before using this object
 */
public Evolver() {
	super();
}

/**
 * Construct new evolver with given properties. See <a href=" {@docRoot}/params.htm"
 * target="anji_params">Parameter Details </a> for specific property settings.
 * @see com.anji.util.Configurable#init(com.anji.util.Properties)
 */
public void init( Properties props ) throws Exception {
	boolean doReset = props.getBooleanProperty( RESET_KEY, false );
	if ( doReset ) {
		logger.warn( "Resetting previous run !!!" );
		Reset resetter = new Reset( props );
		resetter.setUserInteraction( false );
		resetter.reset();
	}

	config = new NeatConfiguration( props );
	ac = new ActivatorTranscriber();
	ac.init(props);
	// peristence
	db = (Persistence) props.singletonObjectProperty( Persistence.PERSISTENCE_CLASS_KEY );

	numEvolutions = props.getIntProperty( NUM_GENERATIONS_KEY );
	targetFitness = props.getDoubleProperty( FITNESS_TARGET_KEY, 1.0d );
	thresholdFitness = props.getDoubleProperty( FITNESS_THRESHOLD_KEY, targetFitness );

	//
	// event listeners
	//

	// run
	// TODO - hibernate
	Run run = (Run) props.singletonObjectProperty( Run.class );
	db.startRun( run.getName() );
	config.getEventManager().addEventListener( GeneticEvent.GENOTYPE_EVALUATED_EVENT, run );

	// logging
	LogEventListener logListener = new LogEventListener( config );
	config.getEventManager().addEventListener( GeneticEvent.GENOTYPE_EVOLVED_EVENT, logListener );
	config.getEventManager()
			.addEventListener( GeneticEvent.GENOTYPE_EVALUATED_EVENT, logListener );

	// persistence
	PersistenceEventListener dbListener = new PersistenceEventListener( config, run );
	dbListener.init( props );
	config.getEventManager().addEventListener(
			GeneticEvent.GENOTYPE_START_GENETIC_OPERATORS_EVENT, dbListener );
	config.getEventManager().addEventListener(
			GeneticEvent.GENOTYPE_FINISH_GENETIC_OPERATORS_EVENT, dbListener );
	config.getEventManager().addEventListener( GeneticEvent.GENOTYPE_EVALUATED_EVENT, dbListener );

	// presentation
	PresentationEventListener presListener = new PresentationEventListener( run );
	presListener.init( props );
	config.getEventManager().addEventListener( GeneticEvent.GENOTYPE_EVALUATED_EVENT,
			presListener );
	config.getEventManager().addEventListener( GeneticEvent.RUN_COMPLETED_EVENT, presListener );

	// fitness function
	BulkFitnessFunction fitnessFunc = (BulkFitnessFunction) props
			.singletonObjectProperty( FITNESS_FUNCTION_CLASS_KEY );
	config.setBulkFitnessFunction( fitnessFunc );
	maxFitness = fitnessFunc.getMaxFitnessValue();

	// load population, either from previous run or random
	genotype = db.loadGenotype( config );
	if ( genotype != null )
		logger.info( "genotype from previous run" );
	else {
		genotype = Genotype.randomInitialGenotype( config );
		logger.info( "random genotype" );
	}//set MYCLASSPATH=.\bin\;.\lib\*.jar;.\lib\jgap.jar;.\lib\log4j.jar;.\lib\jakarta-regexp-1.3.jar;.\lib\clibwrapper_jiio.jar;.\lib\mlibwrapper_jiio.jar;.\lib\jai_imageio.jar;.\lib\hb16.jar;.\lib\jcommon.jar;.\lib\jfreechart.jar;.\lib\jakarta-regexp-1.3.jar;.\properties
	//genotype = Genotype.randomInitialGenotype( config );

}

/**
 * command line usage
 */
private static void usage() {
	System.err.println( "usage: <cmd> <properties-file>" );
}



public static double toDouble(boolean b){ return b?1.0:0.0;}
static ListSet<Double> ALPH = new ListSet<Double>(Arrays.asList(0.0,1.0));
//static FSM<Double> TARGET = FSM.randomFactory(EEAFSM.NUM_STATES, EEAFSM.NUM_ACCEPTING,ALPH);
//static FSM<Double> TARGET = FSM.simpleFactory2(2, ALPH);
//static FSM<Double> TARGET = FSM.iToD(FSM.read("CROSSTARGET"));
//static FSM<Double> TARGET = FSM.countFactory(5,ALPH);
//static FSM<Double> TARGET = FSM.lessThanFactory(10,ALPH);
static FSM<Double> TARGET = FSM.badHandCodedFactory(ALPH);
//static FSM<Double> TARGET = FSM.rangeFactory(6,10,ALPH);
private static boolean BATCH = false;
//static FSM<Double> TARGET = FSM.badHandCodedFactory(ALPH);
static Map<List<Double>,Double> TARGET_BEHAVIOR = new HashMap<List<Double>, Double>();
static Map<List<Double>,Double> labelled = TARGET_BEHAVIOR;
//static Map<List<Double>,Double> labelled = new HashMap<List<Double>,Double>();
List<ExampleGenerator<Double>> RUNS = Arrays.asList(new RandomExample(),new ActiveExample());
static List<List<Double>> ALL_INPUTS = generateAllInputs(EEAFSM.INPUT_LENGTH);

static{
	for(List<Double> l:ALL_INPUTS) TARGET_BEHAVIOR.put(l, toDouble(TARGET.offer(l)));
	labelled = (BATCH)?TARGET_BEHAVIOR:new HashMap<List<Double>,Double>();
}
public double accuracy(Chromosome s){
	double numCorrect = 0;
	for(List<Double> l:TARGET_BEHAVIOR.keySet())
		try{
			if(Math.abs(TARGET_BEHAVIOR.get(l) - ac.newActivator(s).next(listToArray(l))[0]) < .5) ++numCorrect;
		}catch(TranscriberException e){e.printStackTrace();}
	System.err.println(numCorrect+"/"+ALL_INPUTS.size());
	return numCorrect/ALL_INPUTS.size();
}

public double fitness(Chromosome s){
	double numCorrect = 0;
	for(List<Double> l:labelled.keySet())
		try{
			if(Math.abs(labelled.get(l) - ac.newActivator(s).next(listToArray(l))[0]) < .2) ++numCorrect;
		}catch(TranscriberException e){e.printStackTrace();}
	System.err.println(numCorrect+"/"+labelled.size());
	return numCorrect/labelled.size();
}

private static List<List<Double>> generateAllInputs(int length){
	int num = (int)Math.pow(2, length),temp;
	List<List<Double>> all = new ArrayList<List<Double>>(num--);
	LinkedList<Double> one = null;
	for(;num>-1;--num){
		one = new LinkedList<Double>();
		temp = num;
		while(one.size()<length){
			if(temp == 0) one.addFirst(0.0);
			else{
				one.addFirst((double)(temp&1));
				temp>>=1;
			}
		}
		all.add(one);
	}
	return all;
}

/**
 * Perform a single run.
 * 
 * @throws Exception
 */
public void myRun() throws Exception{
	// run start time
	Date runStartDate = Calendar.getInstance().getTime();
	logger.info( "Run: start" );
	DateFormat fmt = new SimpleDateFormat( "HH:mm:ss" );
	TargetFitnessFunction ff = null;
	if(config.getBulkFitnessFunction() instanceof TargetFitnessFunction){
		ff = (TargetFitnessFunction)config.getBulkFitnessFunction();
	}else throw new RuntimeException("EEA without TFF?  No.");
	
	
	for(ExampleGenerator<Double> eg: RUNS){
		if(!BATCH){
			List<Double> firstExample = EEAFSM.randomSentence(EEAFSM.INPUT_LENGTH,ALPH);
			labelled.put(firstExample,toDouble(TARGET.offer(firstExample)));
		}	
		boolean done = false;
		ff.setStimuliAndTarget(labelled);
		// initialize result data
		int generationOfFirstSolution = -1;
		champ = genotype.getFittestChromosome();
		double adjustedFitness = 0;
			//( maxFitness > 0 ? champ.getFitnessValue() / maxFitness : champ.getFitnessValue() );
		for(int iter = 0; iter<100;++iter){
			if(!BATCH)ff.setStimuliAndTarget(labelled);
			adjustedFitness = 0;
			// generations
			for ( int generation = 0; ( generation < numEvolutions && adjustedFitness < 1.0 ); ++generation ) {
				// generation start time
				Date generationStartDate = Calendar.getInstance().getTime();
				//logger.info( "Generation " + generation + ": start" );
			
				// next generation
				try{
					genotype.evolve();
				}catch(IllegalStateException e){ e.printStackTrace();}
			
				// result data
				champ = genotype.getFittestChromosome();
				adjustedFitness = fitness(champ); 
					//( maxFitness > 0 ? (double) champ.getFitnessValue() / maxFitness : champ.getFitnessValue() );
				if ( adjustedFitness >= thresholdFitness && generationOfFirstSolution == -1 )generationOfFirstSolution = generation;
			
				// generation finish
				Date generationEndDate = Calendar.getInstance().getTime();
				long durationMillis = generationEndDate.getTime() - generationStartDate.getTime();
				//logger.info( "Generation " + generation + ": end [" + fmt.format( generationStartDate )
				//		+ " - " + fmt.format( generationEndDate ) + "] [" + durationMillis + "]" );
			}
			if(iter%1==0){
				double accuracy = accuracy(genotype.getFittestChromosome());
				System.err.println("Iteration: "+iter+" of "+eg);
				System.err.println("Max fitness g: "+adjustedFitness);
				System.err.println("Max Accuracy: "+accuracy);
				if(accuracy == 1.0){System.err.println("Solution found!"); done = true; break;}
			}
			if(done) break;
			if(!BATCH){
				List<Double> nextTest = eg.generateExample();
				labelled.put(nextTest, toDouble(TARGET.offer(nextTest)));
			}
		}
		genotype = Genotype.randomInitialGenotype( config ); //RESET GENOTYPE
		labelled.clear(); //RESET LABELLED EXAMPLES
		
		// run finish
		config.getEventManager().fireGeneticEvent(
				new GeneticEvent( GeneticEvent.RUN_COMPLETED_EVENT, genotype ) );
		logConclusion( generationOfFirstSolution, champ );
		Date runEndDate = Calendar.getInstance().getTime();
		long durationMillis = runEndDate.getTime() - runStartDate.getTime();
		logger.info( "Run: end [" + fmt.format( runStartDate ) + " - " + fmt.format( runEndDate )
				+ "] [" + durationMillis + "]" );
		if(BATCH) break;
	}
}

public static final Comparator<Pair<List<Double>,Double>> sentComp = new Comparator<Pair<List<Double>,Double>>(){
	public int compare(Pair<List<Double>,Double> o1, Pair<List<Double>,Double> o2){return o1.second>o2.second?-1:o2.second>o1.second?1:0; }};
public static final class RandomExample implements ExampleGenerator<Double>{
	public static final ExampleGenerator<Double> only = new RandomExample();
	private RandomExample(){}
	public List<Double> generateExample(){return EEAFSM.randomSentence(EEAFSM.INPUT_LENGTH,ALPH);}
	public String toString(){return "Random";}}
public final class ActiveExample implements ExampleGenerator<Double>{
	private ActiveExample(){}
	public List<Double> generateExample(){
		List<Pair<List<Double>,Double>> sentencePop = new LinkedList<Pair<List<Double>,Double>>();
		for(int i = 0;i<EEAFSM.POPULATION_SIZE*EEAFSM.NUM_POPS;++i) sentencePop.add(new Pair<List<Double>,Double>(EEAFSM.randomSentence(EEAFSM.INPUT_LENGTH,ALPH),0.0));
		for(int gen = 0; gen<EEAFSM.MUTATION_GENERATIONS;++gen){
			evaluateSentences(sentencePop);
			Collections.sort(sentencePop,sentComp);
			for(int i =0;i<EEAFSM.ESTIMATION_ITERATIONS;++i){
				int fit = rand.nextInt(sentencePop.size()),unfit = rand.nextInt(sentencePop.size());
				if(fit>unfit){ int temp = fit; fit = unfit; unfit = temp;}
				sentencePop.get(unfit).first = mutateSentence(sentencePop.get(fit).first);
			}
		}
		evaluateSentences(sentencePop);
		double max = -Double.MAX_VALUE;
		List<Double> argmax = null;
		for(Pair<List<Double>,Double> p:sentencePop)
			if(p.second>max){max=p.second; argmax = p.first;}
		return argmax;
	}
	public String toString(){return "Active";}
}

private static Comparator<Chromosome> cComp = new Comparator<Chromosome>(){
	public int compare(Chromosome o1, Chromosome o2){
		return o2.getFitnessValue() - o1.getFitnessValue();
	}
};


private void evaluateSentences(List<Pair<List<Double>, Double>> sentencePop){
	Chromosome[] csomes = new Chromosome[Math.min(4, genotype.getChromosomes().size())];
	List<Chromosome> l = genotype.getChromosomes();
	Collections.sort(l,cComp);
	for(int i = 0; i<csomes.length;++i) csomes[i] = l.get(i);
	for(Pair<List<Double>,Double> p : sentencePop){
		p.second = 0.0;
		double[] stimuli = listToArray(p.first);
		for(Chromosome c:csomes)
			try{
				Activator a = ac.newActivator(c);
				double[] response = a.next(stimuli);
				p.second+=response[0] ;
			}catch(TranscriberException e){e.printStackTrace();}
		p.second = 1.0 - 2.0*Math.abs(.5-p.second/csomes.length);
	}
}
static double[] listToArray(List<Double> l){
	double[] ret = new double[l.size()];
	Iterator<Double> it = l.iterator();
	for(int i = 0;i<ret.length;++i) ret[i] = it.next();
	return ret;
}
static Random rand = new Random(System.nanoTime());
private static List<Double> mutateSentence(List<Double> sent){
	int mutLoc = rand.nextInt(sent.size());
	List<Double> ret = new ArrayList<Double>(sent);
	ret.set(mutLoc, ret.get(mutLoc)==1.0?0.0:1.0);
	return ret;
}
public void run() throws Exception {
	// run start time
	Date runStartDate = Calendar.getInstance().getTime();
	logger.info( "Run: start" );
	DateFormat fmt = new SimpleDateFormat( "HH:mm:ss" );

	// initialize result data
	int generationOfFirstSolution = -1;
	champ = genotype.getFittestChromosome();
	double adjustedFitness = ( maxFitness > 0 ? champ.getFitnessValue() / maxFitness : champ
			.getFitnessValue() );
	// generations
	for ( int generation = 0; ( generation < numEvolutions && adjustedFitness < targetFitness ); ++generation ) {
		// generation start time
		Date generationStartDate = Calendar.getInstance().getTime();
		logger.info( "Generation " + generation + ": start" );
	
		// next generation
		genotype.evolve();
	
		// result data
		champ = genotype.getFittestChromosome();
		adjustedFitness = ( maxFitness > 0 ? (double) champ.getFitnessValue() / maxFitness : champ
				.getFitnessValue() );
		if ( adjustedFitness >= thresholdFitness && generationOfFirstSolution == -1 )
			generationOfFirstSolution = generation;
	
		// generation finish
		Date generationEndDate = Calendar.getInstance().getTime();
		long durationMillis = generationEndDate.getTime() - generationStartDate.getTime();
		logger.info( "Generation " + generation + ": end [" + fmt.format( generationStartDate )
				+ " - " + fmt.format( generationEndDate ) + "] [" + durationMillis + "]" );
	}
	// run finish
	config.getEventManager().fireGeneticEvent(
			new GeneticEvent( GeneticEvent.RUN_COMPLETED_EVENT, genotype ) );
	logConclusion( generationOfFirstSolution, champ );
	Date runEndDate = Calendar.getInstance().getTime();
	long durationMillis = runEndDate.getTime() - runStartDate.getTime();
	logger.info( "Run: end [" + fmt.format( runStartDate ) + " - " + fmt.format( runEndDate )
			+ "] [" + durationMillis + "]" );
}

/**
 * Log summary data of run including generation in which the first solution occurred, and the
 * champion of the final generation.
 * 
 * @param generationOfFirstSolution
 * @param champ
 */
private static void logConclusion( int generationOfFirstSolution, Chromosome champ ) {
	logger.info( "generation of first solution == " + generationOfFirstSolution );
	logger.info( "champ # connections == "
			+ NeatChromosomeUtility.getConnectionList( champ.getAlleles() ).size() );
	logger.info( "champ # hidden nodes == "
			+ NeatChromosomeUtility.getNeuronList( champ.getAlleles(), NeuronType.HIDDEN ).size() );
}

/**
 * Main program used to perform an evolutionary run.
 * 
 * @param args command line arguments; args[0] used as properties file
 * @throws Throwable
 */
public static void main( String[] args ) throws Throwable {
	try {
		System.out.println( "YPOYPYP");

		if ( args.length != 1 ) {
			usage();
			System.exit( -1 );
		}
		System.out.println(new File(args[0]).exists());
		Properties props = new Properties( args[ 0 ] );
		
		Evolver evolver = new Evolver();
		evolver.init( props );
		if(props.getBooleanProperty("eea.run",false)) evolver.myRun();
		else evolver.run();
		System.exit( 0 );
	}
	catch ( Throwable th ) {
		logger.error( "", th );
		throw th;
	}
}

/**
 * @return champion of last generation
 */
public Chromosome getChamp() {
	return champ;
}

/**
 * Fitness of current champ, 0 ... 1
 * @return maximum fitness value
 */
public double getChampAdjustedFitness() {
	return ( champ == null ) ? 0d : (double) champ.getFitnessValue()
			/ config.getBulkFitnessFunction().getMaxFitnessValue();
}

/**
 * @return target fitness value, 0 ... 1
 */
public double getTargetFitness() {
	return targetFitness;
}

/**
 * @return threshold fitness value, 0 ... 1
 */
public double getThresholdFitness() {
	return thresholdFitness;
}

}
