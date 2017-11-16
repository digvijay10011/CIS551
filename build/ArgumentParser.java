import java.util.*;

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
            String[] vals = cmd.getOptionValues(opt);
            if (vals.length > 1) {
                System.exit(255);
            }
            return cmd.getOptionValue(opt);
        }
        return def;
    }

    static boolean hasDuplicateFlags(Option[] options) {
        Set<Character> flags = new HashSet<>();
        for (int i = 0; i < options.length; i++) {
            Option option = options[i];
            char flag = option.getOpt().charAt(0);
            if (flags.contains(flag)) {
                return true;
            }
            flags.add(flag);
        }
        return false;
    }

    static void printInvalidArgs(Options options) {
        // (new HelpFormatter()).printHelp("atm", options);
        System.err.println("Invalid Arguments");
        System.exit(255);
        return;
    }
}
