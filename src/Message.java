
public class Message{
	private String mSender, mMessage;
	private long mTStamp;
	private int messageID;
	private boolean synced;
	
	public Message(String sender, String message, long timestamp, int msgNum) {
		this.mMessage = message;
		this.mSender = sender;
		this.mTStamp = timestamp;
		this.messageID = msgNum;
		this.synced = false;
	}

	public long getmTStamp() {
		return mTStamp;
	}

	public void setmTStamp(int mTStamp) {
		this.mTStamp = mTStamp;
	}

	public String getmSender() {
		return mSender;
	}

	public void setmSender(String mSender) {
		this.mSender = mSender;
	}

	public String getmMessage() {
		return mMessage;
	}

	public void setmMessage(String mMessage) {
		this.mMessage = mMessage;
	}

	public int getMessageID() {
		return messageID;
	}

	public void setMessageID(int messageID) {
		this.messageID = messageID;
	}

	public boolean isSynced() {
		return synced;
	}

	public void setSynced(boolean synced) {
		this.synced = synced;
	}
	
}
