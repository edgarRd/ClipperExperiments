package com.clarkparsia.rpi.clipper;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class TimeReporter {
	
	private String mCurrentQuery;

	Map<String, List<Long>> rwTimes;
	Map<String, List<Long>> evalTimes;
	Map<String, Integer> rwSize;
	Map<String, String> rwType;
	
	private long mCurrentRwTime;
	private boolean activate;
	
	public TimeReporter() {
		rwTimes = new HashMap<String, List<Long>>();
		evalTimes = new HashMap<String, List<Long>>();
		rwSize = new HashMap<String, Integer>();
		rwType = new HashMap<String, String>();
		
		activate = false;
	}
	
	private static class SingletonHolder { 
        public static final TimeReporter INSTANCE = new TimeReporter();
	}
	
	public static TimeReporter getInstance() {
	        return SingletonHolder.INSTANCE;
	}
	
	public void setCurrentQuery(String query) {
		if (this.activate)
			this.mCurrentQuery = query;
	}
	
	public String getCurrentQuery() {
		return mCurrentQuery;
	}
	
	public void addRwTime(long time) {
		if (this.activate) {
			if (!rwTimes.containsKey(mCurrentQuery)) {
				rwTimes.put(mCurrentQuery, new ArrayList<Long>());
			}
			
			rwTimes.get(mCurrentQuery).add(time);
			mCurrentRwTime = time;
		}
	}
	
	public double getAverageRwTime(String theQuery) {
		List<Long> aTimes = rwTimes.get(theQuery);
		long timeSum = 0;
		
		for (Long aVal : aTimes) {
			timeSum += aVal;
		}
		
		return (double)((double)timeSum / (double) aTimes.size());
	}
	
	public double getAverageRwTime() {
		return this.getAverageRwTime(mCurrentQuery);
	}
	
	public void addEvalTime(long time) {
		if (this.activate) {
			if (!evalTimes.containsKey(mCurrentQuery)) {
				evalTimes.put(mCurrentQuery, new ArrayList<Long>());
			}
			
			evalTimes.get(mCurrentQuery).add(time);
		}
	}
	
	public double getAverageEvalTime(String theQuery) {
		List<Long> aTimes = evalTimes.get(theQuery);
		long timeSum = 0;
		
		for (Long aVal : aTimes) {
			timeSum += aVal;
		}
		
		return (double)((double)timeSum / (double) aTimes.size());
	}
	
	public double getAverageEvalTime() {
		return this.getAverageEvalTime(mCurrentQuery);
	}
	
	public long getCurrentRwTime() {
		return mCurrentRwTime;
	}
	
	public void setRwSize(int size) {
		if (this.activate)
			rwSize.put(mCurrentQuery, size);
	}
	
	public int getRwSize(String theQuery) {
		return this.rwSize.get(theQuery);
	}
	
	public int getRwSize() {
		return this.rwSize.get(mCurrentQuery);
	}
	
	public void setRwType(String theType) {
		if (this.activate)
			rwType.put(mCurrentQuery, theType);
	}
	
	public String getRwType(String theQuery) {
		return this.rwType.get(theQuery);
	}
	
	public String getRwType() {
		return this.rwType.get(mCurrentQuery);
	}
	
	public boolean isActive() { return this.activate; }
	
	public void setActive(boolean flag) {
		this.activate = flag;
	}

}
