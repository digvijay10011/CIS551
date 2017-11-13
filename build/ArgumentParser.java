import org.apache.commons.cli.*;

class ArgumentParser {
    static void generateOption(Options options, String opt,
                                    boolean hasArg, String description,
                                    boolean isRequired) {
        Option input = new Option(opt, hasArg, description);
        input.setRequired(isRequired);
        options.addOption(input);
    }

    static String getOptionValue(CommandLine cmd, char opt, String def){
        if (cmd.hasOption(opt)) {
            return cmd.getOptionValue(opt);
        }
        return def;
    }

    static void printInvalidArgs(Options options) {
        (new HelpFormatter()).printHelp("atm", options);
        System.exit(255);
        return;
    }
}
