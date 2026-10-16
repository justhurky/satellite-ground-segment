package io.github.justhurky.groundsegment.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashChainTest {

    private final HashChain hashChain = new HashChain();

    @Test
    void startsFromAGenesisHashOfSixtyFourZeroes() {
        assertThat(HashChain.GENESIS_HASH).hasSize(64).matches("0{64}");
    }

    @Test
    void producesASixtyFourCharacterHexDigest() {
        assertThat(hashChain.link(HashChain.GENESIS_HASH, "1|event")).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void isDeterministicForTheSameInputs() {
        assertThat(hashChain.link(HashChain.GENESIS_HASH, "1|event"))
                .isEqualTo(hashChain.link(HashChain.GENESIS_HASH, "1|event"));
    }

    @Test
    void changesWhenTheRecordChanges() {
        assertThat(hashChain.link(HashChain.GENESIS_HASH, "1|event"))
                .isNotEqualTo(hashChain.link(HashChain.GENESIS_HASH, "1|evenT"));
    }

    @Test
    void changesWhenThePreviousHashChanges() {
        String first = hashChain.link(HashChain.GENESIS_HASH, "1|event");

        assertThat(hashChain.link(first, "2|event")).isNotEqualTo(hashChain.link(HashChain.GENESIS_HASH, "2|event"));
    }

    @Test
    void separatesTheLinkedFieldsUnambiguously() {
        String withSeparator = hashChain.link("ab", "cd");
        String withoutSeparator = hashChain.link("abc", "d");

        assertThat(withSeparator).isNotEqualTo(withoutSeparator);
    }
}
