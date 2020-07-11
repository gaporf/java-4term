package ru.ifmo.rain.akimov.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

//test

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("It's necessary to enter only 2 arguments, the names of input and output file have to be valid");
            return;
        }
        try {
            handle(args[0], args[1]);
        } catch (RecursiveWalkException e) {
            System.err.println(e.getMessage());
        }
    }

    private static String ERROR_HASH = String.format("%08x", 0);

    private static void handle(String nameOfInputFile, String nameOfOutputFile) throws RecursiveWalkException {
        Path input = getPath(nameOfInputFile);
        Path output = getPath(nameOfOutputFile);

        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (FileAlreadyExistsException e) {
            throw new RecursiveWalkException("The parent of the output file is not directory", e);
        } catch (IOException e) {
            throw new RecursiveWalkException("Could not create the output file: " + e.getMessage(), e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            try (BufferedReader reader = Files.newBufferedReader(input)) {
                String nameOfFile;
                while ((nameOfFile = reader.readLine()) != null) {
                    boolean isFinished = false;
                    try {
                        Files.walkFileTree(Paths.get(nameOfFile), new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                writeHash(getHash(file), file.toString(), writer);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                writeHash(ERROR_HASH, file.toString(), writer);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                        isFinished = true;
                    } catch (InvalidPathException e) {
                        System.err.println("The path " + nameOfFile + " has invalid name");
                    } catch (IOException e) {
                        System.err.println("Could not handle the path " + nameOfFile);
                    } finally {
                        if (!isFinished) {
                            writeHash(ERROR_HASH, nameOfFile, writer);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RecursiveWalkException("Could not read the input file: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new RecursiveWalkException("Could not handle the output files: " + e.getMessage(), e);
        }
    }

    private static Path getPath(String nameOfFile) throws RecursiveWalkException {
        try {
            return Paths.get(nameOfFile);
        } catch (InvalidPathException e) {
            throw new RecursiveWalkException("Incorrect path of file " + nameOfFile + ": " + e.getMessage(), e);
        }
    }

    private static void writeHash(String hash, String file, BufferedWriter writer) {
        try {
            writer.write(hash + " " + file + System.lineSeparator());
        } catch (IOException e) {
            throw new RecursiveWalkException("Could not write to the output file: " + e.getMessage(), e);
        }
    }

    private final static int FNV32_PRIME = 0x01000193;

    private static String getHash(Path path) {
        int hval = 0x811c9dc5;
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] bytes = new byte[1024];
            int n;
            while ((n = inputStream.read(bytes)) != -1) {
                for (int i = 0; i < n; i++) {
                    hval *= FNV32_PRIME;
                    hval ^= bytes[i] & 0xff;
                }
            }
        } catch (IOException e) {
            hval = 0;
            System.err.println("Could not get the hash of the file " + path.toString() + ": " + e.getMessage());
        }
        return String.format("%08x", hval);
    }
}
