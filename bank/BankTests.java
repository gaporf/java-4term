package ru.ifmo.rain.akimov.bank;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class BankTests {
    private static Bank bank;
    private final static int PORT = 32381;

    @BeforeAll
    public static void prepare() throws RemoteException {
        final Registry registry = LocateRegistry.createRegistry(PORT);
        bank = new RemoteBank(PORT);
        UnicastRemoteObject.exportObject(bank, PORT);
        registry.rebind("//localhost/bank", bank);
        try {
            bank = (Bank) registry.lookup("//localhost/bank");
        } catch (final NotBoundException ignored) {
        }
    }

    @Test
    public void createPersonTest() throws RemoteException {
        final Person person = bank.createPerson("Vasya", "Pupkin", "VasyaPupkinPassport");
        final Person clone = bank.createPerson("Vasya", "Pupkin", "VasyaPupkinPassport");
        assertEquals(person, clone);
        final Person fakePerson = bank.createPerson("Vitya", "Perestukin", "VasyaPupkinPassport");
        assertNull(fakePerson);
    }

    @Test
    public void checkPersonTest() throws RemoteException {
        bank.createPerson("Sergey", "Sergeev", "SergeySergeevPassport");
        assertTrue(bank.checkPersonByPassport("SergeySergeevPassport", "Sergey", "Sergeev"));
    }

    @Test
    public void createAccountTest() throws RemoteException {
        assertNull(bank.createAccount("V01", "fakeAccount"));
        bank.createPerson("Michael", "Townley", "V01");
        final Account account = bank.createAccount("V01", "funeral");
        final Account clone = bank.createAccount("V01", "funeral");
        assertEquals(account, clone);
    }

    private void createAndGet(final String passportId, final String accountId) throws RemoteException {
        final Account account = bank.createAccount(passportId, accountId);
        assertEquals(account, bank.getAccount(passportId, accountId));
    }

    @Test
    public void getAccountTest() throws RemoteException {
        bank.createPerson("Michael", "De Santa", "V02");
        createAndGet("V02", "house");
        createAndGet("V02", "FIB");
        bank.createPerson("Franklin", "Clinton", "V03");
        createAndGet("V03", "Chop");
        bank.createPerson("Trevor", "Philips", "V04");
        createAndGet("V04", "Trevor Philips Industries");
        assertNull(bank.getAccount("V05", "null"));
        assertNull(bank.getAccount("V01", "FIB"));
    }

    @Test
    public void remoteAccountTest() throws RemoteException {
        bank.createPerson("Gregory", "House", "MH");
        final Account account = bank.createAccount("MH", "vicodin");
        assertEquals(0, account.getAmount());
        account.changeAmount(100);
        assertEquals(100, account.getAmount());
        assertEquals(100, bank.getAccount("MH", "vicodin").getAmount());
        bank.getAccount("MH", "vicodin").changeAmount(200);
        assertEquals(300, account.getAmount());
    }

    @Test
    public void localAccountTest() throws RemoteException {
        bank.createPerson("Sherlock", "Holmes", "SH");
        final Account remoteAccount = bank.createAccount("SH", "cocaine");
        remoteAccount.changeAmount(100);
        final LocalPerson localPerson = bank.getLocalPerson("SH");
        final Account localAccount = localPerson.getAccount("cocaine");
        assertEquals(100, localAccount.getAmount());
        remoteAccount.changeAmount(-200);
        assertEquals(100, localAccount.getAmount());
        assertEquals(-100, remoteAccount.getAmount());
        localAccount.changeAmount(300);
        assertEquals(400, localAccount.getAmount());
        assertEquals(-100, remoteAccount.getAmount());
    }

    private void testSet(final String passportId, final String[] accountIds) throws RemoteException {
        final Set<String> accounts = new HashSet<>();
        for (final String accountId : accountIds) {
            accounts.add(accountId);
            bank.createAccount(passportId, accountId);
        }
        assertEquals(accounts, bank.getAccounts(passportId));
    }

    @Test
    public void getAccountsTest() throws RemoteException {
        bank.createPerson("Ezio", "Auditore", "A");
        testSet("A", new String[]{"Monteriggioni", "pictures", "weapons", "clothes"});
    }

    @Test
    public void personTest() throws RemoteException {
        bank.createPerson("John", "Watson", "JW");
        testSet("JW", new String[]{"house", "family", "work"});
        final LocalPerson localPerson = bank.getLocalPerson("JW");
        final RemotePerson remotePerson = bank.getRemotePerson("JW");
        assertEquals(localPerson.getPassportId(), "JW");
        assertEquals(3, localPerson.getAccounts().size());
        bank.createAccount("JW", "gun");
        assertEquals(3, localPerson.getAccounts().size());
        assertEquals(4, remotePerson.getAccounts().size());
        localPerson.createAccount("wife");
        assertNotEquals(remotePerson.getAccounts(), localPerson.getAccounts());
    }

    @Test
    public void createAccountPersonTest() throws RemoteException {
        final Person person = bank.createPerson("Hank", "Anderson", "DBH");
        person.createAccount("android");
        person.createAccount("alcohol");
        person.createAccount("Sumo");
        assertEquals(3, person.getAccounts().size());
        assertEquals("Sumo", person.getAccount("Sumo").getAccountId());
    }

    @Test
    public void multiThreadTest() throws InterruptedException, RemoteException {
        final ExecutorService checkers = Executors.newCachedThreadPool();
        checkers.submit(() -> {
            try {
                bank.createPerson("Sheldon", "Cooper", "T1");
                bank.createAccount("T1", "comics");
                bank.createAccount("T1", "trains");
                bank.createAccount("T1", "t-shirts");
            } catch (final RemoteException ignored) {
            }
        });
        checkers.submit(() -> {
            try {
                bank.createPerson("Leonard", "Hofstadter", "T2");
                bank.createAccount("T2", "home");
                bank.createAccount("T2", "car");
                bank.createAccount("T2", "university");
            } catch (final RemoteException ignored) {
            }
        });
        checkers.shutdown();
        checkers.awaitTermination(100, TimeUnit.DAYS);
        assertNull(bank.getRemotePerson("T3"));
        assertTrue(bank.checkPersonByPassport("T1", "Sheldon", "Cooper"));
        assertEquals(3, bank.getAccounts("T2").size());
        assertEquals("T1:trains", bank.getAccount("T1", "trains").getFullId());
    }

    @Test
    public void manyThreads() throws InterruptedException {
        final ExecutorService checkers = Executors.newCachedThreadPool();
        for (int thread = 0; thread < 20; thread++) {
            final int numOfThread = thread;
            checkers.submit(() -> {
                try {
                    final String passportId = "passport_Id_" + numOfThread;
                    bank.createPerson("firstName_" + numOfThread, "lastName_" + numOfThread, passportId);
                    for (int account = 0; account < 20; account++) {
                        bank.createAccount(passportId, Integer.toString(account)).changeAmount(account);
                    }
                    for (int account = 0; account < 20; account++) {
                        assertEquals(account, bank.getAccount(passportId, Integer.toString(account)).getAmount());
                    }
                } catch (final RemoteException ignored) {
                }
            });
        }
        checkers.shutdown();
        checkers.awaitTermination(100, TimeUnit.DAYS);
    }

    @Test
    public void manyThreadsAccount() throws InterruptedException, RemoteException {
        final Person person = bank.createPerson("f", "l", "p");
        final Account account = bank.createAccount("p", "a");
        bank.createAccount("p", "aa");
        final ExecutorService checkers = Executors.newCachedThreadPool();
        final int NUM_OF_THREADS = 20;
        final int NUM_OF_OPERATIONS = 100;
        final int DIF = 100;
        for (int thread = 0; thread < NUM_OF_THREADS; thread++) {
            checkers.submit(() -> {
                try {
                    for (int i = 0; i < NUM_OF_OPERATIONS; i++) {
                        account.changeAmount(DIF);
                        person.getAccount("aa").changeAmount(DIF);
                    }
                } catch (final RemoteException ignored) {
                }
            });
        }
        checkers.shutdown();
        checkers.awaitTermination(100, TimeUnit.DAYS);
        assertEquals(DIF * NUM_OF_OPERATIONS * NUM_OF_THREADS, account.getAmount());
        assertEquals(account.getAmount(), person.getAccount("aa").getAmount());
    }

    @Test
    void testClient() throws RemoteException {
        final ExecutorService server = Executors.newFixedThreadPool(1);
        server.submit(() -> Server.main(new String[]{}));
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ignored) {
        }
        final Registry localRegistry = LocateRegistry.getRegistry(32391);
        try {
            final Bank localBank = (Bank) localRegistry.lookup("//localhost/bank");
            Client.main(new String[]{"Dipper", "Pines", "G01", "adventure", "100"});
            assertEquals(100, localBank.getAccount("G01", "adventure").getAmount());
            Client.main(new String[]{"Mayble", "Pines", "G01", "adventure", "150"});
            assertEquals(100, localBank.getAccount("G01", "adventure").getAmount());
            Client.main(null);
            Client.main(new String[]{null, null, null, null, null});
        } catch (final NotBoundException ignored) {
        }
    }

    /*@Test
    public void failTest() {
        assertTrue(false);
    }

    @Test
    public void failTest2() {
        assertFalse(true);
    }*/
}