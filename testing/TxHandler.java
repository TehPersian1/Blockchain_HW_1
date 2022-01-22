import java.util.ArrayList;
import java.util.Arrays;

// CPSC 459 Scrooge Coin Assignment implemented by:
// Shervin Afrasiabi | shervina@csu.fullerton.edu | Section 01
// Prateek Gupta     | pgupta24@csu.fullerton.edu | Section 01


public class TxHandler {

	/* Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is utxoPool. This should make a defensive copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */

	private UTXOPool _utxoPool;

	public TxHandler(UTXOPool utxoPool) 	{
		this._utxoPool = new UTXOPool(utxoPool);
	}

	/* Returns true if
	 * (1) all outputs claimed by tx are in the current UTXO pool,
	 * (2) the signatures on each input of tx are valid,
	 * (3) no UTXO is claimed multiple times by tx,
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of
	        its output values;
	   and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {
		ArrayList<UTXO> oldutxo = new ArrayList();
		double inputsum = 0;
		double outputsum = 0;

		for(int i=0; i<tx.numInputs(); i++) {
			Transaction.Input input = tx.getInput(i);
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			// rule #1 - All outputs claimed by the TX are in the current UTXO pool
			if (this._utxoPool.contains(utxo) == false)
				return false;// transaction not in the utxo pool

			// rule #2 - The signatures on each input of a transaction are valid
			Transaction.Output output = this._utxoPool.getTxOutput(utxo);
			if (output == null) {
				return false;
			} // utxo is not in the pool

			RSAKey pKey = output.address;
			byte[] message = tx.getRawDataToSign(i);
			byte[] signature = input.signature;
			if (!pKey.verifySignature(message, signature)) {
				return false;
			}// signature fails to validate

			// rule #3 - No UTXO is claimed multiple times by a transaction
			if (oldutxo.contains(utxo)) {
				return false;
			}// a transaction is claimed multiple times
			oldutxo.add(utxo);
			inputsum += output.value;
		}

		// rule #4 - All of the transactions output values are non-negative
		for (int i=0;i<tx.numOutputs();i++) {
			Transaction.Output output = tx.getOutput(i);
			if (output.value < 0) {
				return false;
			} // an output is negative
			outputsum += output.value;
		}
		
		// rule #5 - The sum of transactions input values is greater than or equal to the sum of its output values
		if (inputsum < outputsum) {
			return false; // overspending
		}
		// if we manage to get to this point, we have "passed every test" and can verify the transaction.isvalid()
		// and the utxo pool can be updated 
		return true; // valid transaction
	}

	/* Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness,
	 * returning a mutually valid array of accepted transactions,
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction> validTransactions = new ArrayList<>();
		for(int i=0; i<possibleTxs.length;i++){
			if(isValidTx(possibleTxs[i])){ // implementing our function created above on each transaction
				validTransactions.add(possibleTxs[i]);
				// if the transaction is valid handle it by:

				//remove the old utxo
				for (Transaction.Input input : possibleTxs[i].getInputs()) {
					UTXO oldutxo = new UTXO(input.prevTxHash, input.outputIndex);
					this._utxoPool.removeUTXO(oldutxo);
				}
				// get hash from previous transaction, update the utxo pool with the newUTXO at the current index
				byte[] hash = possibleTxs[i].getHash();
				int index = 0;
				for (Transaction.Output output : possibleTxs[i].getOutputs()){
					UTXO newutxo = new UTXO(hash, index);
					index++;
					this._utxoPool.addUTXO(newutxo, output);
				}
			}
		}
		// get the final array of validated transactions and return
		Transaction[] finalizedTransactions = validTransactions.toArray(new Transaction[validTransactions.size()]);
		return finalizedTransactions;
	}

}
