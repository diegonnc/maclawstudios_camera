/************************************************************************/
/* MaclawStudios Camera App for Samsung Galaxy Ace and Gio              */
/* Copyright (C) 2012 Marcin Chojnacki & MaclawStudios                  */
/*                                                                      */
/* This program is free software: you can redistribute it and/or modify */
/* it under the terms of the GNU General Public License as published by */
/* the Free Software Foundation, either version 3 of the License, or    */
/* (at your option) any later version.                                  */
/*                                                                      */
/* This program is distributed in the hope that it will be useful,      */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of       */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the         */
/* GNU General Public License for more details.                         */
/*                                                                      */
/* You should have received a copy of the GNU General Public License    */
/* along with this program.  If not, see <http://www.gnu.org/licenses/> */
/************************************************************************/

package com.galaxyics.camera;

import java.io.File;
import java.util.Calendar;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class Name {
	private MediaScannerConnection m;
	
	public Name() {
		createIfNotExists("/sdcard/DCIM");
        createIfNotExists("/sdcard/DCIM/Camera");
	}
	
    public String getName() {
    	Calendar c=Calendar.getInstance();
    	return String.format("/sdcard/DCIM/Camera/IMG_%d%s%s_%s%s%s.jpg",c.get(Calendar.YEAR),
    											 generateLong(c.get(Calendar.DATE)),
    											 generateLong(c.get(Calendar.MONTH)),
    											 generateLong(c.get(Calendar.HOUR_OF_DAY)),
    											 generateLong(c.get(Calendar.MINUTE)),
    											 generateLong(c.get(Calendar.SECOND)));
	}
    
    public void scanFile(Context c,final String s) {
        m=new MediaScannerConnection(c,new MediaScannerConnectionClient() {
        	public void onMediaScannerConnected() {
        		m.scanFile(s,"image/jpeg");
            }

			public void onScanCompleted(String arg0,Uri arg1) {
				//do nothing
			}
        });
        m.connect();
    }
    
    private void createIfNotExists(String s) {
        File dir=new File(s);
        if(!dir.exists()) dir.mkdir();
    }
    
    private String generateLong(int v) {
    	String tmp=Integer.toString(v);
    	if(tmp.length()==1) tmp="0"+tmp;
    	return tmp;
    }
}
