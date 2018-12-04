package io.carpe.hyperscan.wrapper;

import io.carpe.hyperscan.db.ChimeraDatabase;
import io.carpe.hyperscan.db.CompileErrorException;
import io.carpe.hyperscan.jna.HyperscanLibrary;
import io.carpe.hyperscan.wrapper.flags.ChimeraExpressionFlag;
import io.carpe.hyperscan.wrapper.flags.HyperscanExpressionFlag;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class ChimeraTest {

    @Test
    void simpleSingleExpression() throws HyperscanException {
        final EnumSet<ChimeraExpressionFlag> flags = EnumSet.of(ChimeraExpressionFlag.CASELESS, ChimeraExpressionFlag.EXTRACT_MATCHED);
        final ChimeraExpression expression = new ChimeraExpression("(?>(?:\\b(I really don't like).{0,50}))*\\K(*SKIP)(?<!\\S|not )\\bThe Penny[!]", flags);
        final ChimeraExpression.ValidationResult result = expression.validate();

        assertThat(result.isValid()).isTrue();
        assertThat(result.isValidHyperscan()).isFalse();
        assertThat(result.isValidChimera()).isTrue();
        assertThat(result.getChimeraErrorMessage()).isEmpty();

        try (final ChimeraDatabase db = ChimeraDatabase.compile(expression)) {

            assertThat(db.getSize()).isGreaterThan(0);
            final Scanner scanner = new Scanner();
            scanner.allocScratch(db);

            // assert the scanner has loaded the chimera db into scratch space
            assertThat(scanner.getChimeraSize()).isGreaterThan(0);

            // run matcher
            final List<Match> matches = scanner.scan(db, "i really like the penny!");

            // assert the only expression that matched was the one we created in this test
            final List<Expression> matchedExpressions = matches.stream()
                    .map(Match::getMatchedExpression)
                    .collect(Collectors.toList());
            Assertions.assertThat(matchedExpressions).containsExactly(expression);

            assertThat(matches).hasSize(1);
            assertThat(matches.get(0).getStartPosition()).isEqualTo(14);
            assertThat(matches.get(0).getEndPosition()).isEqualTo(24);
            assertThat(matches.get(0).getMatchedString()).isEqualTo("the penny!");
        }
    }

    @Test
    void simpleMultiExpression() throws HyperscanException {
        final LinkedList<ChimeraExpression> expressions = new LinkedList<>();

        final EnumSet<ChimeraExpressionFlag> flags = EnumSet.of(ChimeraExpressionFlag.CASELESS);

        final ChimeraExpression pcreExpression = new ChimeraExpression("(?>(?:\\b(I really don't like).{0,50}))*\\K(*SKIP)(?<!\\S|not )\\bThe Penny[!]", flags);
        expressions.add(pcreExpression);

        final ChimeraExpression hyperscanExpression = new ChimeraExpression("really", flags);
        expressions.add(hyperscanExpression);

        try (final ChimeraDatabase db = ChimeraDatabase.compile(expressions)) {
            assertThat(db.getSize()).isGreaterThan(0);
            final Scanner scanner = new Scanner();

            // allocate scratch for db
            scanner.allocScratch(db);

            // assert the scanner has loaded the chimera expression into scratch space
            assertThat(scanner.getChimeraSize()).isGreaterThan(0);

            // assert the scanner has loaded the hyperscan expression into scratch space
//            assertThat(scanner.getHyperscanSize()).isGreaterThan(0);

            final List<Match> matches = scanner.scan(db, "i really really like the penny! really!");

            // assert we got 4 matches
            assertThat(matches).hasSize(4);

            // assert the both expressions got matched
            final List<Expression> matchedExpressions = matches.stream()
                    .map(Match::getMatchedExpression)
                    .collect(Collectors.toList());
            Assertions.assertThat(matchedExpressions).containsExactlyInAnyOrder(pcreExpression, hyperscanExpression, hyperscanExpression, hyperscanExpression);
        }
    }

    /**
     * Checks if Regex Strings can be validated
     */
    @Test
    void expressionsCanBeValidated() {
        final ChimeraExpression invalidExpression = new ChimeraExpression("test\\1");
        final ChimeraExpression.ValidationResult invalidResult = invalidExpression.validate();
        assertThat(invalidResult.isValid()).isFalse();

        // check simple bools
        assertThat(invalidResult.isValidHyperscan()).isFalse();
        assertThat(invalidResult.isValidChimera()).isFalse();

        // check hyperscan validity
        assertThat(invalidResult.getHyperscanErrorMessage()).isPresent();
        assertThat(invalidResult.getHyperscanErrorMessage()).contains("Invalid back reference to expression 1.");

        // check chimera validity
        assertThat(invalidResult.getChimeraErrorMessage()).isPresent();
        assertThat(invalidResult.getChimeraErrorMessage()).contains("PCRE compilation failed: reference to non-existent subpattern.");

        // on compile an exception should be thrown
        assertThatExceptionOfType(CompileErrorException.class)
                .isThrownBy(() -> ChimeraDatabase.compile(invalidExpression));

    }

    @Test
    void version() {
        Assertions.assertThat(HyperscanLibrary.INSTANCE.hs_version()).isEqualTo("5.0.0 2018-11-29");
    }

    @Test
    void infiniteRegex() throws HyperscanException {
        final ChimeraDatabase db = ChimeraDatabase.compile(new ChimeraExpression("a|", EnumSet.of(ChimeraExpressionFlag.SINGLEMATCH)));
        final Scanner scanner = new Scanner();
        scanner.allocScratch(db);
        scanner.scan(db, "12345 test string");
    }

    @Test
    void doesIt() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> ChimeraDatabase.compile(new ChimeraExpression(null)));
    }


    @Test
    void readmeExample() throws HyperscanException {
        //we define a list containing all of our expressions
        final LinkedList<ChimeraExpression> expressions = new LinkedList<>();

        //the first argument in the constructor is the regular pattern, the latter one is a expression flag
        //make sure you read the original hyperscan documentation to learn more about flags
        //or browse the HyperscanExpressionFlag.java in this repo.
        expressions.add(new ChimeraExpression("[0-9]{5}", EnumSet.of(ChimeraExpressionFlag.SINGLEMATCH)));
        expressions.add(new ChimeraExpression("Test", EnumSet.of(ChimeraExpressionFlag.CASELESS)));


        //we precompile the expression into a database.
        //you can compile single expression instances or lists of expressions

        //since we're interacting with native handles always use try-with-resources or call the close method after use
        try (final ChimeraDatabase db = ChimeraDatabase.compile(expressions)) {
            //initialize scanner - one scanner per thread!
            //same here, always use try-with-resources or call the close method after use
            try (final Scanner scanner = new Scanner()) {
                //allocate scratch space matching the passed database
                scanner.allocScratch(db);

                //provide the database and the input string
                //returns a list with matches
                //synchronized method, only one execution at a time (use more scanner instances for multithreading)
                final List<Match> matches = scanner.scan(db, "12345 test string");

                //matches always contain the expression causing the match and the end position of the match
                //the start position and the matches string it self is only part of a matach if the
                //SOM_LEFTMOST is set (for more details refer to the original hyperscan documentation)
                assertThat(matches).hasSize(2);
            }
        }
    }


    @Test
    void chineseUTF8() throws HyperscanException {
        final ChimeraExpression expr = new ChimeraExpression("测试", EnumSet.of(ChimeraExpressionFlag.UTF8));
        try (ChimeraDatabase db = ChimeraDatabase.compile(expr)) {
            final Scanner scanner = new Scanner();
            scanner.allocScratch(db);
            final List<Match> matches = scanner.scan(db, "这是一个测试");

            assertThat(matches).hasSize(1);
        }
    }

    @Test
    void utf8MatchedString() throws HyperscanException {
        final ChimeraExpression expr = new ChimeraExpression("\\d{5}", EnumSet.of(HyperscanExpressionFlag.UTF8));
        try (ChimeraDatabase db = ChimeraDatabase.compile(expr)) {
            final Scanner scanner = new Scanner();
            scanner.allocScratch(db);
            final List<Match> matches = scanner.scan(db, " Menu About Us Strategy Professionals Investments Contact Contact Home / Contact 1 2 Map DataMap data ©2017 GoogleMap DataMap data ©2017 GoogleMap data ©2017 GoogleTerms of UseReport a map errorMapCustom Map RFE Investment Partners 36 Grove St New Canaan, CT, 06840 (203) 966-2800 For general inquiries: info@rfeip.com For intermediaries: deals@rfeip.com For executives: executives@rfeip.com For investors: investors@rfeip.com Copyright 2016 RFE Investment Partners Premium Barbershop is the prime spot for your hair grooming needs in find us at 75250 FAIRWAY Drive Indian Wells, CA 92210 open 24/7 New York City. Our approach is simple and efficient. We are here to provide the best hair cut, shave, or any other grooming service you may desire! Menu HOME ABOUT US SERVICES GALLERY BLOG SOCIAL CONTACT US HOME ABOUT US SERVICES GALLERY BLOG SOCIAL CONTACT US We are open 7 days (855) 692 2887 latest news Enjoy a limited time $5 discount by printing the voucher below Premium Barbershop is growing. Read More We Open A New Location on 622 3rd avenue (Lobby) (bet. 40st and 41st) Read More What we offer Manhattan barber shop Best New York barbershop Premium Barbershop always offers professional quality for all of our customers and we are ready to deal with your highest expectations. Are you looking for quality? You found it! Our services are dedicated for your personal success. Here at Premium Barbershop we have award winning staff that have demonstrated talent of master barbers at several notable styling competitions. Let our barber to be your personal stylist and you will never be disappointed. In addition we place your personal style vision above all the other things. Our master barbers always ready to give you professional advices but will also ask you about all the cut to achieve a most desirable result for you. Most of our visitors are our regular clients now. They include celebrities, business executives and many other people who want to look good and make a proper impression. Our professional service and our care about their notion makes them to leave with a smile on their faces and totally satisfied. Many of our clients claims it was a best New York barbershop, they visit. Most accessible Manhattan barber shop Our modernly equipped Barbershop is located in one step away from the business center of Manhattan – on 299 East 52nd street, between 1st and 2nd Ave. We are open 7 days a week from early morning until evening, making it possible to get a haircut during the hours most convenient for you. We won`t waste even one moment of your time. We do our work, you enjoy your time and your style. While we take care of providing you with the best style you can; watch hot political and economic news of the world on large flat screen TVs, or for sports fans we show the latest UFC and Mixed Marshall Arts Championship programs. Here at Premium Barbershop we respect your time and try our best for our services to be most accessible, most enjoyable and convenient Manhattan barber shop ever. Working Hours Mon-Fri: 8:30 AM – 7:30 PM Saturday: 9:00 AM – 6:00 PM Sunday: 10:00 AM – 5:00 PM Save $5 OFF Services Haircut services Shampoo + Cut $24.95 Long Layered Cut $24.95 Regular Haircut $21.95 Fade + Hot Towel $21.95 Children’s Haircut $18.95 Crew Cut + Shape-Up $21.95 Senior Citizen Cut $17.95 Crew Cut + Hot Towel $17.95 RAZOR SERVICES Shave $24.95 Beard Trim $9.95 Beard Trim with Razor $12.95 Clean-Up $9.95 Goatee Beard $5.95 OUR LOCATIONS 299 East 52nd street (bet. 1st and 2nd Ave) New York, NY 10022 (212) 935 - 3066 (Read More) 134 1/2 East 62nd Street (bet. Lexington & 3rd Ave) New York, NY 10021 (212) 308 - 6660 (Read More) 622 3rd avenue (Lobby) (bet. 40st and 41st) New York, NY 10017 (646) 649 - 2235 (Read More) Home About us Blog Contact us Gallery Services Social @2017 Premium Barber Shop. All Rights Reserved. ");

            assertThat(matches.get(0).getMatched()).isEmpty();
            assertThat(matches.get(0).getMatchedString()).isNull();
        }
    }
}