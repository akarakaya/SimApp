package org.fog.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.PhysicalTopology;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.placement.MyModulePlacement;
import org.fog.utils.JsonToTopology;
import org.fog.utils.TimeKeeper;

/**
 * Simulation setup for EEG Beam Tractor Game extracting physical topology 
 * @author Harshit Gupta
 *
 */
public class CleanFromJson {

//	static List<Integer> idOfEndDevices = new ArrayList<Integer>();
//	static Map<Integer,FogDevice> deviceById = new HashMap<Integer,FogDevice>();
//	static Map<Integer, Map<String, Double>> deadlineInfo = new HashMap<Integer, Map<String, Double>>();
//	static Map<Integer, Map<String, Integer>> additionalMipsInfo = new HashMap<Integer, Map<String, Integer>>();
	
	public static void main(String[] args) {

		Log.printLine("Starting Health and Tactical Analysis System (HaTAS)...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "HaTAS";
			
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
//			moduleMapping.addModuleToDevice("tuner", "cloud");
//			for(int i=0;i<idOfEndDevices.size();i++)
//			{
//				FogDevice fogDevice = deviceById.get(idOfEndDevices.get(i));
//				moduleMapping.addModuleToDevice("classifier", fogDevice.getName()); 
//			}
			/*
			 * Creating the physical topology from specified JSON file
			 * Topolojiyi dosyadan alir.
			 */
			PhysicalTopology physicalTopology = JsonToTopology.getPhysicalTopology(broker.getId(), appId, "topologies/HaTAS");
						
			Controller controller = new Controller("master-controller", physicalTopology.getFogDevices(), physicalTopology.getSensors(), 
					physicalTopology.getActuators());
			
			/*
			 * uygulamada hazirlanan tum cihazlar ve sistem bu asamada submit edilir.
			 * moduleplacementedgewars yapisina gore cihazlara atama yapilir.
			 * Bu yapi uzerinde algoritma isletilecek
			 */
			 controller.submitApplication(application, 0, new MyModulePlacement(physicalTopology.getFogDevices(), 
					physicalTopology.getSensors(), physicalTopology.getActuators(), 
					application, moduleMapping,"classifier"));
			
			/* 
			 * controller.submitApplication(application, 0, new ModulePlacementMapping(physicalTopology.getFogDevices(),
					application, ModuleMapping.createModuleMapping()));
			*/
			 
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("HaTAS finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}
	
	
//	private static double getvalue(double min, double max)
//	{
//		Random r = new Random();
//		double randomValue = min + (max - min) * r.nextDouble();
//		return randomValue;
//	}
//	
//	private static int getvalue(int min, int max)
//	{
//		Random r = new Random();
//		int randomValue = min + r.nextInt()%(max - min);
//		return randomValue;
//	}
	
	// Application da kullanilan durumlar tanimlanir
	@SuppressWarnings({ "serial" })
	private static Application createApplication(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId);
		application.addAppModule("client", 10, 1000, 1000, 100);
		application.addAppModule("classifier", 50, 1500, 4000, 800);
		application.addAppModule("tuner", 10, 50, 12000, 100);
		
		// buradaki sira tekrar duzenlenecek
		application.addTupleMapping("client", "ECG", "_SENSOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "SENSORS", "_SENSOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "CLASSIFICATION", "ACTUATOR", new FractionalSelectivity(1.0));
		//application.addTupleMapping("classifier", "_SENSOR", "CLASSIFICATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "HISTORY", new FractionalSelectivity(0.1));
		application.addTupleMapping("tuner", "HISTORY", "TUNING_PARAMS", new FractionalSelectivity(1.0));
	
		application.addAppEdge("ECG", "client", 1000, 100, "ECG", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("SENSORS", "client", 1000, 100, "SENSORS", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client", "classifier", 1000, 100, "_SENSOR", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("classifier", "tuner", 1000, 100, "HISTORY", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("classifier", "client", 1000, 100, "CLASSIFICATION", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("tuner", "classifier", 1000, 100, "TUNING_PARAMS", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("client", "DISPLAY", 1000, 100, "ACTUATOR", Tuple.DOWN, AppEdge.ACTUATOR);
		
//		for(int id:idOfEndDevices)
//		{
//			Map<String,Double>moduleDeadline = new HashMap<String,Double>();
//			moduleDeadline.put("mainModule", getvalue(3.00, 5.00));
//			Map<String,Integer>moduleAddMips = new HashMap<String,Integer>();
//			moduleAddMips.put("mainModule", getvalue(0, 500));
//			deadlineInfo.put(id, moduleDeadline);
//			additionalMipsInfo.put(id,moduleAddMips);
//			
//		}
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("ECG");add("SENSORS");add("client");add("classifier");add("client");add("DISPLAY");}});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("classifier");add("tuner");add("classifier");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};
		
		application.setLoops(loops);
//		application.setDeadlineInfo(deadlineInfo);
//		application.setAdditionalMipsInfo(additionalMipsInfo);
		
		//GeoCoverage geoCoverage = new GeoCoverage(-100, 100, -100, 100);
		return application;
	}
}