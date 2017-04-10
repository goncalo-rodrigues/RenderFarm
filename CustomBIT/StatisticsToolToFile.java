//
// StatisticsToolToFile.java
//
// This program measures and instruments to obtain different statistics
// about Java programs.
//
// Copyright (c) 1998 by Han B. Lee (hanlee@cs.colorado.edu).
// ALL RIGHTS RESERVED.
//
// Permission to use, copy, modify, and distribute this software and its
// documentation for non-commercial purposes is hereby granted provided
// that this copyright notice appears in all copies.
//
// This software is provided "as is".  The licensor makes no warrenties, either
// expressed or implied, about its correctness or performance.  The licensor
// shall not be liable for any damages suffered as a result of using
// and modifying this software.

import BIT.highBIT.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;
import java.util.HashMap;

public class StatisticsToolToFile
{
	private static HashMap<Long, DynamicMetrics> dyn = new HashMap<Long, DynamicMetrics>();

	private static int newcount = 0;
	private static int newarraycount = 0;
	private static int anewarraycount = 0;
	private static int multianewarraycount = 0;

	private static int loadcount = 0;
	private static int storecount = 0;
	private static int fieldloadcount = 0;
	private static int fieldstorecount = 0;

	private static StatisticsBranchToFile[] branch_info;
	private static int branch_number;
	private static int branch_pc;
	private static String branch_class_name;
	private static String branch_method_name;

	public static void printUsage()
		{
			System.out.println("Syntax: java StatisticsToolToFile -stat_type in_path [out_path]");
			System.out.println("        where stat_type can be:");
			System.out.println("        static:     static properties");
			System.out.println("        dynamic:    dynamic properties");
			System.out.println("        alloc:      memory allocation instructions");
			System.out.println("        load_store: loads and stores (both field and regular)");
			System.out.println("        branch:     gathers branch outcome statistics");
			System.out.println();
			System.out.println("        in_path:  directory from which the class files are read");
			System.out.println("        out_path: directory to which the class files are written");
			System.out.println("        Both in_path and out_path are required unless stat_type is static");
			System.out.println("        in which case only in_path is required");
			System.exit(-1);
		}

	public static void doStatic(File in_dir)
		{
			String filelist[] = in_dir.list();
			int method_count = 0;
			int bb_count = 0;
			int instr_count = 0;
			int class_count = 0;

			for (int i = 0; i < filelist.length; i++) {
				String filename = filelist[i];
				if (filename.endsWith(".class")) {
					class_count++;
					String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					ClassInfo ci = new ClassInfo(in_filename);
					Vector routines = ci.getRoutines();
					method_count += routines.size();

					for (Enumeration e = routines.elements(); e.hasMoreElements(); ) {
						Routine routine = (Routine) e.nextElement();
						BasicBlockArray bba = routine.getBasicBlocks();
						bb_count += bba.size();
						InstructionArray ia = routine.getInstructionArray();
						instr_count += ia.size();
					}
				}
			}

			/*
			 *  Changed the way of outputting the information
			 */

			long threadId = Thread.currentThread().getId();

			try{
				PrintWriter writer = new PrintWriter("static_" + threadId + ".txt", "UTF-8");

				writer.println("Static information summary:");
				writer.println("Number of class files:  " + class_count);
				writer.println("Number of methods:      " + method_count);
				writer.println("Number of basic blocks: " + bb_count);
				writer.println("Number of instructions: " + instr_count);
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			/*
			 *  End of the changed instructions
			 */

			if (class_count == 0 || method_count == 0) {
				return;
			}

			float instr_per_bb = (float) instr_count / (float) bb_count;
			float instr_per_method = (float) instr_count / (float) method_count;
			float instr_per_class = (float) instr_count / (float) class_count;
			float bb_per_method = (float) bb_count / (float) method_count;
			float bb_per_class = (float) bb_count / (float) class_count;
			float method_per_class = (float) method_count / (float) class_count;

			/*
			 *  Changed the way of outputting the information
			 */

			try{
				PrintWriter writer = new PrintWriter(new FileOutputStream(
						new File("static_" + threadId + ".txt"), true));

				writer.println();
				writer.println("Average number of instructions per basic block: " + instr_per_bb);
				writer.println("Average number of instructions per method:      " + instr_per_method);
				writer.println("Average number of instructions per class:       " + instr_per_class);
				writer.println("Average number of basic blocks per method:      " + bb_per_method);
				writer.println("Average number of basic blocks per class:       " + bb_per_class);
				writer.println("Average number of methods per class:            " + method_per_class);
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			/*
			 *  End of the changed instructions
			 */

		}

	public static void doDynamic(File in_dir, File out_dir)
		{
			String filelist[] = in_dir.list();

			for (int i = 0; i < filelist.length; i++) {
				String filename = filelist[i];
				if (filename.endsWith(".class")) {
					String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					ClassInfo ci = new ClassInfo(in_filename);
					for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
						Routine routine = (Routine) e.nextElement();
						routine.addBefore("StatisticsToolToFile", "dynMethodCount", new Integer(1));

						for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
							BasicBlock bb = (BasicBlock) b.nextElement();
							bb.addBefore("StatisticsToolToFile", "dynInstrCount", new Integer(bb.size()));
						}
					}
					ci.addAfter("StatisticsToolToFile", "printDynamic", "null");
					ci.write(out_filename);
				}
			}
		}

    public static synchronized void printDynamic(String foo)
		{
			/*
			 *  Changed the way of outputting the information
			 */

			long threadId = Thread.currentThread().getId();
			DynamicMetrics dm = dyn.get(threadId);
			long dyn_method_count = dm.getMethodCount();
			long dyn_bb_count = dm.getBBCount();
			long dyn_instr_count = dm.getInstrCount();

			try{
				PrintWriter writer = new PrintWriter("dynamic_" + threadId + ".txt", "UTF-8");

				writer.println("Dynamic information summary:");
				writer.println("Number of methods:      " + dyn_method_count);
				writer.println("Number of basic blocks: " + dyn_bb_count);
				writer.println("Number of instructions: " + dyn_instr_count);
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			/*
			 *  End of the changed instructions
			 */

			if (dyn_method_count == 0) {
				dyn.remove(threadId);
				return;
			}

			double instr_per_bb = (double) dyn_instr_count / dyn_bb_count;
			double instr_per_method = (double) dyn_instr_count / dyn_method_count;
			double bb_per_method = (double) dyn_bb_count / dyn_method_count;

			/*
			 *  Changed the way of outputting the information
			 */

			try{
				PrintWriter writer = new PrintWriter(new FileOutputStream(
						new File("dynamic_" + threadId + ".txt"), true));

				writer.println();
				writer.println("Average number of instructions per basic block: " + instr_per_bb);
				writer.println("Average number of instructions per method:      " + instr_per_method);
				writer.println("Average number of basic blocks per method:      " + bb_per_method);
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			dyn.remove(threadId);

			/*
			 *  End of the changed instructions
			 */
		}


    public static void dynInstrCount(int incr)
		{
			long threadId = Thread.currentThread().getId();

			synchronized (dyn) {
				if (!dyn.containsKey(threadId))
					dyn.put(threadId, new DynamicMetrics());
			}

			DynamicMetrics dm = dyn.get(threadId);
			dm.incIntrCount(incr);
			dm.incBBCount(1);
		}

    public static void dynMethodCount(int incr)
		{
			long threadId = Thread.currentThread().getId();

			synchronized (dyn) {
				if (!dyn.containsKey(threadId))
					dyn.put(threadId, new DynamicMetrics());
			}

			dyn.get(threadId).incMethodCount(1);
		}

	public static void doAlloc(File in_dir, File out_dir)
		{
			String filelist[] = in_dir.list();

			for (int i = 0; i < filelist.length; i++) {
				String filename = filelist[i];
				if (filename.endsWith(".class")) {
					String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					ClassInfo ci = new ClassInfo(in_filename);

					for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
						Routine routine = (Routine) e.nextElement();
						InstructionArray instructions = routine.getInstructionArray();

						for (Enumeration instrs = instructions.elements(); instrs.hasMoreElements(); ) {
							Instruction instr = (Instruction) instrs.nextElement();
							int opcode=instr.getOpcode();
							if ((opcode==InstructionTable.NEW) ||
								(opcode==InstructionTable.newarray) ||
								(opcode==InstructionTable.anewarray) ||
								(opcode==InstructionTable.multianewarray)) {
								instr.addBefore("StatisticsToolToFile", "allocCount", new Integer(opcode));
							}
						}
					}
					ci.addAfter("StatisticsToolToFile", "printAlloc", "null");
					ci.write(out_filename);
				}
			}
		}

	public static synchronized void printAlloc(String s)
		{
			/*
			 *  Changed the way of outputting the information
			 */

			long threadId = Thread.currentThread().getId();

			try{
				PrintWriter writer = new PrintWriter("alloc_" + threadId + ".txt", "UTF-8");

				writer.println("Allocations summary:");
				writer.println("new:            " + newcount);
				writer.println("newarray:       " + newarraycount);
				writer.println("anewarray:      " + anewarraycount);
				writer.println("multianewarray: " + multianewarraycount);
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			/*
			 *  End of the changed instructions
			 */
		}

	public static synchronized void allocCount(int type)
		{
			switch(type) {
			case InstructionTable.NEW:
				newcount++;
				break;
			case InstructionTable.newarray:
				newarraycount++;
				break;
			case InstructionTable.anewarray:
				anewarraycount++;
				break;
			case InstructionTable.multianewarray:
				multianewarraycount++;
				break;
			}
		}

	public static void doLoadStore(File in_dir, File out_dir)
		{
			String filelist[] = in_dir.list();

			for (int i = 0; i < filelist.length; i++) {
				String filename = filelist[i];
				if (filename.endsWith(".class")) {
					String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					ClassInfo ci = new ClassInfo(in_filename);

					for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
						Routine routine = (Routine) e.nextElement();

						for (Enumeration instrs = (routine.getInstructionArray()).elements(); instrs.hasMoreElements(); ) {
							Instruction instr = (Instruction) instrs.nextElement();
							int opcode=instr.getOpcode();
							if (opcode == InstructionTable.getfield)
								instr.addBefore("StatisticsToolToFile", "LSFieldCount", new Integer(0));
							else if (opcode == InstructionTable.putfield)
								instr.addBefore("StatisticsToolToFile", "LSFieldCount", new Integer(1));
							else {
								short instr_type = InstructionTable.InstructionTypeTable[opcode];
								if (instr_type == InstructionTable.LOAD_INSTRUCTION) {
									instr.addBefore("StatisticsToolToFile", "LSCount", new Integer(0));
								}
								else if (instr_type == InstructionTable.STORE_INSTRUCTION) {
									instr.addBefore("StatisticsToolToFile", "LSCount", new Integer(1));
								}
							}
						}
					}
					ci.addAfter("StatisticsToolToFile", "printLoadStore", "null");
					ci.write(out_filename);
				}
			}
		}

	public static synchronized void printLoadStore(String s)
		{
			/*
			 *  Changed the way of outputting the information
			 */

			long threadId = Thread.currentThread().getId();

			try{
				PrintWriter writer = new PrintWriter("loadstore_" + threadId + ".txt", "UTF-8");

				writer.println("Load Store Summary:");
				writer.println("Field load:    " + fieldloadcount);
				writer.println("Field store:   " + fieldstorecount);
				writer.println("Regular load:  " + loadcount);
				writer.println("Regular store: " + storecount);
				writer.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			/*
			 *  End of the changed instructions
			 */
		}

	public static synchronized void LSFieldCount(int type)
		{
			if (type == 0)
				fieldloadcount++;
			else
				fieldstorecount++;
		}

	public static synchronized void LSCount(int type)
		{
			if (type == 0)
				loadcount++;
			else
				storecount++;
		}

	public static void doBranch(File in_dir, File out_dir)
		{
			String filelist[] = in_dir.list();
			int k = 0;
			int total = 0;

			for (int i = 0; i < filelist.length; i++) {
				String filename = filelist[i];
				if (filename.endsWith(".class")) {
					String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					ClassInfo ci = new ClassInfo(in_filename);

					for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
						Routine routine = (Routine) e.nextElement();
						InstructionArray instructions = routine.getInstructionArray();
						for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
							BasicBlock bb = (BasicBlock) b.nextElement();
							Instruction instr = (Instruction) instructions.elementAt(bb.getEndAddress());
							short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
							if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
								total++;
							}
						}
					}
				}
			}

			for (int i = 0; i < filelist.length; i++) {
				String filename = filelist[i];
				if (filename.endsWith(".class")) {
					String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
					ClassInfo ci = new ClassInfo(in_filename);

					for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
						Routine routine = (Routine) e.nextElement();
						routine.addBefore("StatisticsToolToFile", "setBranchMethodName", routine.getMethodName());
						InstructionArray instructions = routine.getInstructionArray();
						for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
							BasicBlock bb = (BasicBlock) b.nextElement();
							Instruction instr = (Instruction) instructions.elementAt(bb.getEndAddress());
							short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
							if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
								instr.addBefore("StatisticsToolToFile", "setBranchPC", new Integer(instr.getOffset()));
								instr.addBefore("StatisticsToolToFile", "updateBranchNumber", new Integer(k));
								instr.addBefore("StatisticsToolToFile", "updateBranchOutcome", "BranchOutcome");
								k++;
							}
						}
					}
					ci.addBefore("StatisticsToolToFile", "setBranchClassName", ci.getClassName());
					ci.addBefore("StatisticsToolToFile", "branchInit", new Integer(total));
					ci.addAfter("StatisticsToolToFile", "printBranch", "null");
					ci.write(out_filename);
				}
			}
		}

	public static synchronized void setBranchClassName(String name)
		{
			branch_class_name = name;
		}

	public static synchronized void setBranchMethodName(String name)
		{
			branch_method_name = name;
		}

	public static synchronized void setBranchPC(int pc)
		{
			branch_pc = pc;
		}

	public static synchronized void branchInit(int n)
		{
			if (branch_info == null) {
				branch_info = new StatisticsBranchToFile[n];
			}
		}

	public static synchronized void updateBranchNumber(int n)
		{
			branch_number = n;

			if (branch_info[branch_number] == null) {
				branch_info[branch_number] = new StatisticsBranchToFile(branch_class_name, branch_method_name, branch_pc);
			}
		}

	public static synchronized void updateBranchOutcome(int br_outcome)
		{
			if (br_outcome == 0) {
				branch_info[branch_number].incrNotTaken();
			}
			else {
				branch_info[branch_number].incrTaken();
			}
		}

	public static synchronized void printBranch(String foo)
		{
			/*
			 *  Changed the way of outputting the information
			 */

			long threadId = Thread.currentThread().getId();
			PrintWriter writer = null;

			try{
				writer = new PrintWriter("branch_" + threadId + ".txt", "UTF-8");

				writer.println("Branch summary:");
				writer.println("CLASS NAME" + '\t' + "METHOD" + '\t' + "PC" + '\t' + "TAKEN" + '\t' + "NOT_TAKEN");

			} catch (IOException e) {
				e.printStackTrace();
			}

			for (int i = 0; i < branch_info.length; i++) {
				if (branch_info[i] != null) {
					branch_info[i].print(writer);
				}
			}

			writer.close();

			/*
			 *  End of the changed instructions
			 */
		}


	public static void main(String argv[])
		{
			if (argv.length < 2 || !argv[0].startsWith("-")) {
				printUsage();
			}

			if (argv[0].equals("-static")) {
				if (argv.length != 2) {
					printUsage();
				}

				try {
					File in_dir = new File(argv[1]);

					if (in_dir.isDirectory()) {
						doStatic(in_dir);
					}
					else {
						printUsage();
					}
				}
				catch (NullPointerException e) {
					printUsage();
				}
			}

			else if (argv[0].equals("-dynamic")) {
				if (argv.length != 3) {
					printUsage();
				}

				try {
					File in_dir = new File(argv[1]);
					File out_dir = new File(argv[2]);

					if (in_dir.isDirectory() && out_dir.isDirectory()) {
						doDynamic(in_dir, out_dir);
					}
					else {
						printUsage();
					}
				}
				catch (NullPointerException e) {
					printUsage();
				}
			}

			else if (argv[0].equals("-alloc")) {
				if (argv.length != 3) {
					printUsage();
				}

				try {
					File in_dir = new File(argv[1]);
					File out_dir = new File(argv[2]);

					if (in_dir.isDirectory() && out_dir.isDirectory()) {
						doAlloc(in_dir, out_dir);
					}
					else {
						printUsage();
					}
				}
				catch (NullPointerException e) {
					printUsage();
				}
			}

			else if (argv[0].equals("-load_store")) {
				if (argv.length != 3) {
					printUsage();
				}

				try {
					File in_dir = new File(argv[1]);
					File out_dir = new File(argv[2]);

					if (in_dir.isDirectory() && out_dir.isDirectory()) {
						doLoadStore(in_dir, out_dir);
					}
					else {
						printUsage();
					}
				}
				catch (NullPointerException e) {
					printUsage();
				}
			}

			else if (argv[0].equals("-branch")) {
				if (argv.length != 3) {
					printUsage();
				}

				try {
					File in_dir = new File(argv[1]);
					File out_dir = new File(argv[2]);

					if (in_dir.isDirectory() && out_dir.isDirectory()) {
						doBranch(in_dir, out_dir);
					}
					else {
						printUsage();
					}
				}
				catch (NullPointerException e) {
					printUsage();
				}
			}
		}
}
