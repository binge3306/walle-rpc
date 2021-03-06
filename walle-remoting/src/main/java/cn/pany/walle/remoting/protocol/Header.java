/*
 * Copyright 2018-2019 Pany Young.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.pany.walle.remoting.protocol;

import cn.pany.walle.common.protocol.MessageType;

import java.util.HashMap;
import java.util.Map;

/** 
 * @author pany
 * @version 1.0  
 * @createDate：2015年12月16日 上午11:24:40 
 * 
 */
public class Header {
	private int crcCode=0xabef0101;
	
	private int length;//消息长度

	private long sessionID;//回话ID
	
	private MessageType type;//消息类型
	
	private byte priority;//消息优先级
	
	private Map<String,String> attachment=new HashMap<String, String>();//附件

	public int getCrcCode() {
		return crcCode;
	}

	public void setCrcCode(int crcCode) {
		this.crcCode = crcCode;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public long getSessionID() {
		return sessionID;
	}

	public void setSessionID(long sessionID) {
		this.sessionID = sessionID;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public byte getPriority() {
		return priority;
	}

	public void setPriority(byte priority) {
		this.priority = priority;
	}

	public Map<String, String> getAttachment() {
		return attachment;
	}

	public void setAttachment(Map<String, String> attachment) {
		this.attachment = attachment;
	}

	@Override
	public String toString() {
		return "Header [crcCode=" + crcCode + ", length=" + length
				+ ", sessionID=" + sessionID + ", type=" + type + ", priority="
				+ priority + ", attachment=" + attachment + "]";
	}
	
}
