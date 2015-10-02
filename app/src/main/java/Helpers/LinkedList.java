package Helpers;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("serial")
public class LinkedList implements Serializable {

	public class UpdateObject {
		public String pred;
		public String succ;

		public UpdateObject(String p, String s) {
			this.pred = p;
			this.succ = s;
		}
	}

	LinkedListNode root;

	public LinkedList(LinkedListNode node) {
		this.root = node;
	}

	public int getLength() {
		LinkedListNode node = this.root;
		int length = 0;

		do {
			node = node.getNextNode();
			length++;
		} while (node != this.root);

		return length;
	}

	public void insertNodeIntoList(String portNumber) {

		/* Divide port no by 2 to get the AVD no and use that for node joins */
		String nodeJoin = String.valueOf(Integer.valueOf(portNumber) / 2);

		LinkedListNode node = getCorrectNodeGivenKeyOrPortNumber(nodeJoin);

		LinkedListNode newNode = new LinkedListNode(portNumber, node, node.getPreviousNode());
		node.getPreviousNode().setNextNode(newNode);
		node.setPreviousNode(newNode);
	}

	public LinkedListNode getNodeForPortNumber(String portNo) {

		LinkedListNode retNode = null;
		LinkedListNode itr = this.root;

		do {
			if (itr.getPortNumber().equals(portNo)) {
				retNode = itr;
				break;
			}
			itr = itr.getNextNode();
		} while (itr != root);

		return retNode;
	}

	public String getNextPortNumber(String portNo) {

		LinkedListNode itr = this.root;

		do {
			if (itr.getPortNumber().equals(portNo)) {
				break;
			}
			itr = itr.getNextNode();
		} while (itr != root);

		return itr.getNextNode().getPortNumber();
	}

	public String getCoordinatorForKey(String key) {
		return getCorrectNodeGivenKeyOrPortNumber(key).getPortNumber();
	}

	public String getDeleteNode(String key) {
		return getCorrectNodeGivenKeyOrPortNumber(key).getPortNumber();
	}

	private LinkedListNode getCorrectNodeGivenKeyOrPortNumber(String keyOrPort) {

		boolean couldInsert = false;
		String portHashed = null;
		LinkedListNode retNode = null;

		try {
			portHashed = AppData.genHash(keyOrPort);
			
			LinkedListNode node = this.root;
			String maxPortNoHash = node.getPortHashedNumber();
			String maxPortNo = node.getPortNumber();

			do {
				String prevHash = node.getPreviousNode().getPortHashedNumber();
				String myHash = node.getPortHashedNumber();

				if ((portHashed.compareTo(prevHash) > 0) && (portHashed.compareTo(myHash) <= 0)) {
					couldInsert = true;
					break;
				}

				if (maxPortNoHash.compareTo(node.getPortHashedNumber()) < 0) {
					maxPortNoHash = node.getPortHashedNumber();
					maxPortNo = node.getPortNumber();
				}

				node = node.getNextNode();
			} while (node != root);

			if (!couldInsert) {
				/* Get the node after the max and insert between this node and the max node */
				node = getNodeForPortNumber(maxPortNo).getNextNode();
			}
			
			retNode = node;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return retNode;
	}

}
