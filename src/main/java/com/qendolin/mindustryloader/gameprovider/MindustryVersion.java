package com.qendolin.mindustryloader.gameprovider;

import net.fabricmc.loader.impl.util.version.StringVersion;

public class MindustryVersion {
    /**
     * Build type. 'official' for official releases; 'custom' or 'bleeding edge' are also used.
     */
    public final String type;
    /**
     * Build modifier, e.g. 'alpha' or 'release'
     */
    public final String modifier;
    /**
     * Number specifying the major version, e.g. '4'
     */
    public final int number;
    /**
     * Build number, e.g. '43'. set to '-1' for custom builds.
     */
    public final int build;
    /**
     * Revision number. Used for hotfixes. Does not affect server compatibility.
     */
    public final int revision;

    private MindustryVersion(String type, String modifier, int number, int build, int revision) {
        this.type = type;
        this.modifier = modifier;
        this.number = number;
        this.build = build;
        this.revision = revision;
    }

    @Override
    public String toString() {
        return "MindustryVersion{" +
                "type='" + type + '\'' +
                ", modifier='" + modifier + '\'' +
                ", number=" + number +
                ", build=" + build +
                ", revision=" + revision +
                '}';
    }

    public StringVersion toStringVersion() {
        String semVer = number + "." + build + "." + revision;
        if (modifier != null && !modifier.isEmpty() && !modifier.equals("unknown")) {
            semVer += "-" + modifier.replace("-", "");
        }
        return new StringVersion(semVer);
    }

    public static final class Builder {
        private String type = "unknown";
        private String modifier = "unknown";
        private int number = 0;
        private int build = 0;
        private int revision = 0;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setModifier(String modifier) {
            this.modifier = modifier;
            return this;
        }

        public Builder setNumber(int number) {
            this.number = number;
            return this;
        }

        public Builder setBuild(int build) {
            this.build = build;
            return this;
        }

        public Builder setRevision(int revision) {
            this.revision = revision;
            return this;
        }

        public MindustryVersion build() {
            return new MindustryVersion(type, modifier, number, build, revision);
        }
    }
}
