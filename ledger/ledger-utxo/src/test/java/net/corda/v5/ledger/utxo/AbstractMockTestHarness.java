package net.corda.v5.ledger.utxo;

import net.corda.v5.application.crypto.DigitalSignatureAndMetadata;
import net.corda.v5.application.crypto.DigitalSignatureMetadata;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.crypto.DigitalSignature;
import net.corda.v5.crypto.SecureHash;
import net.corda.v5.ledger.common.Party;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction;
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder;
import org.mockito.Mockito;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

public class AbstractMockTestHarness {

    protected static class Create implements Command {
    }

    protected static class Update implements Command {
    }

    protected final PublicKey aliceKey = Mockito.mock(PublicKey.class);
    protected final PublicKey bobKey = Mockito.mock(PublicKey.class);
    protected final PublicKey notaryKey = Mockito.mock(PublicKey.class);

    protected final Attachment attachment = Mockito.mock(Attachment.class);
    protected final ZipInputStream zipInputStream = Mockito.mock(ZipInputStream.class);
    protected final OutputStream outputStream = Mockito.mock(OutputStream.class);
    protected final InputStream inputStream = Mockito.mock(InputStream.class);
    protected final Contract contract = Mockito.mock(Contract.class);
    protected final Command command = Mockito.mock(Command.class);
    protected final TimeWindow timeWindow = Mockito.mock(TimeWindow.class);

    protected final Instant minInstant = Instant.MIN;
    protected final Instant maxInstant = Instant.MIN;
    protected final Instant midpoint = Instant.EPOCH;
    protected final SecureHash hash = SecureHash.parse("SHA256:0000000000000000000000000000000000000000000000000000000000000000");
    protected final List<PublicKey> keys = List.of(aliceKey, bobKey);
    protected final MemberX500Name notaryName = new MemberX500Name("Notary", "Zurich", "CH");
    protected final DigitalSignatureAndMetadata aliceSignature = createDigitalSignature(aliceKey);
    protected final DigitalSignatureAndMetadata bobSignature = createDigitalSignature(bobKey);
    protected final List<DigitalSignatureAndMetadata> signatures = List.of(aliceSignature);
    protected final Command createCommand = new Create();
    protected final Command updateCommand = new Update();
    protected final List<Command> commands = List.of(command, createCommand, updateCommand);
    protected final StateRef stateRef = new StateRef(hash, 0);
    protected final Party notaryParty = new Party(notaryName, notaryKey);

    protected final StateAndRef<ContractState> contractStateAndRef = createStateAndRef(Mockito.mock(ContractState.class));
    protected final TransactionState<ContractState> contractTransactionState = contractStateAndRef.getState();
    protected final ContractState contractState = contractTransactionState.getContractState();

    protected final UtxoLedgerService utxoLedgerService = Mockito.mock(UtxoLedgerService.class);
    protected final UtxoLedgerTransaction utxoLedgerTransaction = Mockito.mock(UtxoLedgerTransaction.class);
    protected final UtxoSignedTransaction utxoSignedTransaction = Mockito.mock(UtxoSignedTransaction.class);
    protected final UtxoTransactionBuilder utxoTransactionBuilder = Mockito.mock(UtxoTransactionBuilder.class);

    public AbstractMockTestHarness() {
        initializeContract();
        initializeContractState();
        initializeAttachment();
        initializeTimeWindow();
        initializeUtxoLedgerService();
        initializeUtxoLedgerTransaction();
        initializeUtxoSignedTransaction();
        initializeUtxoTransactionBuilder();
    }

    private <T extends ContractState> TransactionState<T> createTransactionState(T contractState) {
        TransactionState<T> result = Mockito.mock(TransactionState.class);

        Mockito.when(result.getContractState()).thenReturn(contractState);
        Mockito.when(result.getContractType()).thenReturn((Class) contract.getClass());
        Mockito.when(result.getNotary()).thenReturn(new Party(notaryName, notaryKey));
        Mockito.when(result.getEncumbrance()).thenReturn(0);

        return result;
    }

    private <T extends ContractState> StateAndRef<T> createStateAndRef(T contractState) {
        StateAndRef<T> result = Mockito.mock(StateAndRef.class);

        SecureHash hash = SecureHash.parse("SHA256:0000000000000000000000000000000000000000000000000000000000000000");
        int index = 0;
        StateRef ref = new StateRef(hash, index);

        TransactionState<T> transactionState = createTransactionState(contractState);
        Mockito.when(result.getState()).thenReturn(transactionState);
        Mockito.when(result.getRef()).thenReturn(ref);

        return result;
    }

    private DigitalSignatureAndMetadata createDigitalSignature(PublicKey signatory) {
        DigitalSignature.WithKey signature = new DigitalSignature.WithKey(signatory, new byte[]{0}, Map.of());
        DigitalSignatureMetadata metadata = new DigitalSignatureMetadata(minInstant, Map.of());

        return new DigitalSignatureAndMetadata(signature, metadata);
    }

    private void initializeContract() {
        Mockito.doNothing().when(contract).verify(utxoLedgerTransaction);
    }

    private void initializeContractState() {
        Mockito.when(contractState.getParticipants()).thenReturn(keys);
    }

    private void initializeAttachment() {
        Mockito.when(attachment.getId()).thenReturn(hash);
        Mockito.when(attachment.getSize()).thenReturn(0);
        Mockito.when(attachment.getSignatories()).thenReturn(keys);
        Mockito.when(attachment.open()).thenReturn(inputStream);
        Mockito.when(attachment.openAsZip()).thenReturn(zipInputStream);
    }

    private void initializeTimeWindow() {
        Mockito.when(timeWindow.getFrom()).thenReturn(minInstant);
        Mockito.when(timeWindow.getUntil()).thenReturn(maxInstant);
        Mockito.when(timeWindow.contains(midpoint)).thenReturn(true);
    }

    private void initializeUtxoLedgerService() {
        Mockito.when(utxoLedgerService.getTransactionBuilder()).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoLedgerService.resolve(List.of(stateRef))).thenReturn(List.of(contractStateAndRef));
        Mockito.when(utxoLedgerService.resolve(stateRef)).thenReturn(contractStateAndRef);
        Mockito.doNothing().when(utxoLedgerService).verify(List.of(contractStateAndRef));
        Mockito.doNothing().when(utxoLedgerService).verify(contractStateAndRef);
    }

    private void initializeUtxoLedgerTransaction() {
        Mockito.when(utxoLedgerTransaction.getTimeWindow()).thenReturn(timeWindow);
        Mockito.when(utxoLedgerTransaction.getAttachments()).thenReturn(List.of(attachment));
        Mockito.when(utxoLedgerTransaction.getCommands()).thenReturn(commands);
        Mockito.when(utxoLedgerTransaction.getSignatories()).thenReturn(keys);
        Mockito.when(utxoLedgerTransaction.getInputStateAndRefs()).thenReturn(List.of(contractStateAndRef));
        Mockito.when(utxoLedgerTransaction.getInputTransactionStates()).thenCallRealMethod();
        Mockito.when(utxoLedgerTransaction.getInputContractStates()).thenCallRealMethod();
        Mockito.when(utxoLedgerTransaction.getReferenceInputStateAndRefs()).thenReturn(List.of(contractStateAndRef));
        Mockito.when(utxoLedgerTransaction.getReferenceInputTransactionStates()).thenCallRealMethod();
        Mockito.when(utxoLedgerTransaction.getReferenceInputContractStates()).thenCallRealMethod();
        Mockito.when(utxoLedgerTransaction.getOutputStateAndRefs()).thenReturn(List.of(contractStateAndRef));
        Mockito.when(utxoLedgerTransaction.getOutputTransactionStates()).thenCallRealMethod();
        Mockito.when(utxoLedgerTransaction.getOutputContractStates()).thenCallRealMethod();
        Mockito.doNothing().when(utxoLedgerTransaction).verify();
        Mockito.when(utxoLedgerTransaction.getAttachment(hash)).thenReturn(attachment);
        Mockito.when(utxoLedgerTransaction.getCommands(Create.class)).thenReturn((List) List.of(createCommand));
        Mockito.when(utxoLedgerTransaction.getCommands(Update.class)).thenReturn((List) List.of(updateCommand));
        Mockito.when(utxoLedgerTransaction.getInputStateAndRefs(ContractState.class)).thenReturn(List.of(contractStateAndRef));
        Mockito.when(utxoLedgerTransaction.getInputStates(ContractState.class)).thenReturn(List.of(contractState));
        Mockito.when(utxoLedgerTransaction.getReferenceInputStateAndRefs(ContractState.class)).thenReturn(List.of(contractStateAndRef));
        Mockito.when(utxoLedgerTransaction.getReferenceInputStates(ContractState.class)).thenReturn(List.of(contractState));
        Mockito.when(utxoLedgerTransaction.getOutputStateAndRefs(ContractState.class)).thenReturn(List.of(contractStateAndRef));
        Mockito.when(utxoLedgerTransaction.getOutputStates(ContractState.class)).thenReturn(List.of(contractState));
    }

    private void initializeUtxoSignedTransaction() {
        Mockito.when(utxoSignedTransaction.getId()).thenReturn(hash);
        Mockito.when(utxoSignedTransaction.getSignatures()).thenReturn(signatures);
        Mockito.when(utxoSignedTransaction.addSignatures(List.of(aliceSignature, bobSignature))).thenReturn(utxoSignedTransaction);
        Mockito.when(utxoSignedTransaction.addSignatures(aliceSignature, bobSignature)).thenReturn(utxoSignedTransaction);
        Mockito.when(utxoSignedTransaction.getMissingSignatories()).thenReturn(keys);
        Mockito.when(utxoSignedTransaction.toLedgerTransaction()).thenReturn(utxoLedgerTransaction);
    }

    private void initializeUtxoTransactionBuilder() {
        Mockito.when(utxoTransactionBuilder.getNotary()).thenReturn(notaryParty);
        Mockito.when(utxoTransactionBuilder.addAttachment(hash)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addCommand(createCommand)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addCommand(updateCommand)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addSignatories(keys)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addCommandAndSignatories(createCommand, keys)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addCommandAndSignatories(updateCommand, keys)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addCommandAndSignatories(createCommand, aliceKey, bobKey)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addCommandAndSignatories(updateCommand, aliceKey, bobKey)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addInputState(contractStateAndRef)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addReferenceInputState(contractStateAndRef)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addOutputState(contractState)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.addOutputState(contractState, 0)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.setTimeWindowUntil(maxInstant)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.setTimeWindowBetween(minInstant, maxInstant)).thenReturn(utxoTransactionBuilder);
        Mockito.when(utxoTransactionBuilder.sign()).thenReturn(utxoSignedTransaction);
        Mockito.when(utxoTransactionBuilder.sign(keys)).thenReturn(utxoSignedTransaction);
        Mockito.when(utxoTransactionBuilder.sign(aliceKey, bobKey)).thenReturn(utxoSignedTransaction);
        Mockito.doNothing().when(utxoTransactionBuilder).verify();
    }
}
