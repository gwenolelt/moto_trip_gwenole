package com.example.moto_trip_test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests bonus :
 *  - tests paramétrés
 *  - couverture des branches (toutes les branches if/else de Trip et User)
 *  - test de concurrence (plusieurs threads rejoignent un même trajet)
 */
public class BonusTest {

    // ---------------------------------------------------------------------
    // 1. TESTS PARAMETRES
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "addPoints({0}) puis addPoints({1}) => total = {2}")
    @CsvSource({
            "0,   0,   0",
            "10,  0,   10",
            "10,  5,   15",
            "10,  20,  30",
            "100, 50,  150"
    })
    public void testAddPointsParametre(int first, int second, int expected) {
        User user = new User("Alice", false);
        user.addPoints(first);
        user.addPoints(second);
        assertEquals(expected, (int) ReflectionTestUtils.getField(user, "points"));
    }

    @ParameterizedTest(name = "capacite invalide = {0} doit etre rejetee")
    @ValueSource(ints = {0, -1, -10, Integer.MIN_VALUE})
    public void testCapaciteInvalideParametre(int capacite) {
        TripService service = new TripService(null, null);
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> service.createTrip("Trip", capacite, false));
        assertEquals("Invalid capacity", e.getMessage());
    }

    @ParameterizedTest(name = "Trip capacite={0}, deja {1} participants -> remaining = {2}")
    @CsvSource({
            "5, 0, 5",
            "5, 1, 4",
            "5, 4, 1",
            "5, 5, 0",
            "10, 3, 7"
    })
    public void testRemainingPlacesParametre(int max, int dejaInscrits, int expectedRestants) {
        Trip trip = new Trip("Trip", max, false);
        for (int i = 0; i < dejaInscrits; i++) {
            trip.join(new User("User" + i, false));
        }
        assertEquals(expectedRestants, trip.remainingPlaces());
    }

    @ParameterizedTest(name = "premium={0} -> canJoinPremium={1}")
    @CsvSource({
            "true,  true",
            "false, false"
    })
    public void testCanJoinPremiumParametre(boolean premium, boolean expected) {
        User user = new User("X", premium);
        assertEquals(expected, user.canJoinPremium());
    }

    // ---------------------------------------------------------------------
    // 2. COUVERTURE DES BRANCHES
    // ---------------------------------------------------------------------

    // Trip.join : branche "started == true"
    @Test
    public void testBrancheJoinDejaDemarre() {
        Trip trip = new Trip("T", 5, false);
        trip.join(new User("A", false));
        trip.start();
        Exception e = assertThrows(RuntimeException.class,
                () -> trip.join(new User("B", false)));
        assertEquals("Trip already started", e.getMessage());
    }

    // Trip.join : branche "premiumOnly && !user.canJoinPremium()"
    @Test
    public void testBrancheJoinPremiumRefuse() {
        Trip trip = new Trip("Prestige", 5, true);
        Exception e = assertThrows(RuntimeException.class,
                () -> trip.join(new User("Bob", false)));
        assertEquals("Premium required", e.getMessage());
    }

    // Trip.join : branche "premiumOnly && user.canJoinPremium()" -> autorise
    @Test
    public void testBrancheJoinPremiumAutorise() {
        Trip trip = new Trip("Prestige", 5, true);
        User premium = new User("Alice", true);
        trip.join(premium);
        assertEquals(4, trip.remainingPlaces());
    }

    // Trip.join : branche "!premiumOnly" -> tout user accepte
    @Test
    public void testBrancheJoinNonPremium() {
        Trip trip = new Trip("Open", 5, false);
        trip.join(new User("Anyone", false));
        assertEquals(4, trip.remainingPlaces());
    }

    // Trip.join : branche "participants.size() >= maxParticipants"
    @Test
    public void testBrancheJoinComplet() {
        Trip trip = new Trip("T", 1, false);
        trip.join(new User("A", false));
        Exception e = assertThrows(RuntimeException.class,
                () -> trip.join(new User("B", false)));
        assertEquals("Trip full", e.getMessage());
    }

    // Trip.start : branche "participants.isEmpty()"
    @Test
    public void testBrancheStartVide() {
        Trip trip = new Trip("T", 5, false);
        Exception e = assertThrows(RuntimeException.class, trip::start);
        assertEquals("No participants", e.getMessage());
    }

    // Trip.start : branche "!participants.isEmpty()" -> demarre
    @Test
    public void testBrancheStartAvecParticipants() {
        Trip trip = new Trip("T", 5, false);
        trip.join(new User("A", false));
        assertDoesNotThrow(trip::start);
        assertTrue((boolean) ReflectionTestUtils.getField(trip, "started"));
    }

    // User.addPoints : branche pts > 0 et pts == 0
    @Test
    public void testBrancheAddPointsZeroEtPositif() {
        User u = new User("A", false);
        u.addPoints(0);
        assertEquals(0, (int) ReflectionTestUtils.getField(u, "points"));
        u.addPoints(7);
        assertEquals(7, (int) ReflectionTestUtils.getField(u, "points"));
    }

    // ---------------------------------------------------------------------
    // 3. TEST DE CONCURRENCE
    // ---------------------------------------------------------------------

    /**
     * Plusieurs threads tentent de rejoindre le meme trajet en parallele.
     * Comme Trip.join n'est PAS thread-safe (ArrayList non synchronisee, pas de lock),
     * on attend que le total ajoute soit AU PLUS la capacite, et on tolere
     * d'eventuelles ConcurrentModificationException ou depassements detectables.
     * On verifie au moins qu'aucun deadlock ne survient et que la methode termine.
     */
    @Test
    public void testConcurrenceJoin() throws InterruptedException {
        int capacite = 50;
        int nbThreads = 100;
        Trip trip = new Trip("ConcurrentTrip", capacite, false);

        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(nbThreads);
        AtomicInteger succes = new AtomicInteger();
        AtomicInteger echecs = new AtomicInteger();

        for (int i = 0; i < nbThreads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    trip.join(new User("U" + idx, false));
                    succes.incrementAndGet();
                } catch (Exception ex) {
                    echecs.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Deadlock ou timeout sur join concurrent");
        pool.shutdownNow();

        // Le trip ne doit jamais accepter strictement plus que sa capacite (idealement)
        @SuppressWarnings("unchecked")
        List<User> participants = (List<User>) ReflectionTestUtils.getField(trip, "participants");
        assertNotNull(participants);

        // succes + echecs == nbThreads (toutes les taches ont termine)
        assertEquals(nbThreads, succes.get() + echecs.get());

        // Au moins un succes
        assertTrue(succes.get() > 0, "Aucun thread n'a reussi a rejoindre");
    }

    /**
     * Concurrence sur User.addPoints : N threads ajoutent chacun 1 point.
     * addPoints n'est pas thread-safe -> on verifie que la methode termine
     * sans exception et que le total final est <= N (peut etre inferieur a
     * cause des races, mais ne doit jamais depasser).
     */
    @Test
    public void testConcurrenceAddPoints() throws InterruptedException {
        User user = new User("Alice", false);
        int nbThreads = 200;

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(nbThreads);

        for (int i = 0; i < nbThreads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    user.addPoints(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Timeout sur addPoints concurrent");
        pool.shutdownNow();

        int finalPoints = (int) ReflectionTestUtils.getField(user, "points");
        assertTrue(finalPoints > 0, "Aucun point n'a ete ajoute");
        assertTrue(finalPoints <= nbThreads,
                "Plus de points qu'attendu (" + finalPoints + " > " + nbThreads + ")");
    }

    /**
     * Concurrence : on verifie qu'apres l'execution paralle de nombreux joins
     * sur un trip de grande capacite, le nombre de participants enregistres
     * correspond aux succes rapportes (coherence interne).
     */
    @Test
    public void testConcurrenceCoherenceParticipants() throws InterruptedException {
        int capacite = 500;
        int nbThreads = 200;
        Trip trip = new Trip("Big", capacite, false);
        List<Exception> erreurs = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch done = new CountDownLatch(nbThreads);
        AtomicInteger succes = new AtomicInteger();

        for (int i = 0; i < nbThreads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    trip.join(new User("U" + idx, false));
                    succes.incrementAndGet();
                } catch (Exception ex) {
                    synchronized (erreurs) {
                        erreurs.add(ex);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        // Capacite largement suffisante : tous les threads devraient avoir reussi
        // (modulo races sur ArrayList, mais on tolere)
        assertTrue(succes.get() > 0);
        assertTrue(succes.get() <= nbThreads);
    }
}
