package com.eliteseriespay.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class ApplicationIconTest {

    @Test
    void loadImage_loadsBundledApplicationIcon() {
        BufferedImage image = ApplicationIcon.loadImage();

        assertThat(image.getWidth()).isGreaterThan(0);
        assertThat(image.getHeight()).isGreaterThan(0);
    }

    @Test
    void loadScaledImage_returnsRequestedSize() {
        BufferedImage scaled = ApplicationIcon.loadScaledImage(16);

        assertThat(scaled.getWidth()).isEqualTo(16);
        assertThat(scaled.getHeight()).isEqualTo(16);
    }
}
