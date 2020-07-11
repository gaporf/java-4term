package ru.ifmo.rain.akimov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//test3

public class StudentDB implements AdvancedStudentGroupQuery {

    private static final Comparator<Student> COMPARATOR_NAME = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    private static final Comparator<Group> COMPARATOR_GROUP = Comparator.comparing(Group::getName);

    private static final Comparator<Group> COMPARATOR_COUNT_OF_STUDENTS = Comparator
            .comparing((Group group) -> group.getStudents().size()).reversed()
            .thenComparing(Group::getName);

    private static final Comparator<Map.Entry<String, Set<String>>> COMPARATOR_SET = Comparator
            .comparingInt((Map.Entry<String, Set<String>> name) -> name.getValue().size());

    private static final Comparator<Map.Entry<String, Set<String>>> COMPARATOR_FIRST_NAMES =
            COMPARATOR_SET.reversed().thenComparing(Map.Entry::getKey);

    private static final Comparator<Map.Entry<String, Set<String>>> COMPARATOR_POPULAR_NAMES =
            COMPARATOR_SET.thenComparing(Map.Entry::getKey);

    private <T> List<T> streamToList(Stream<T> stream) {
        return stream.collect(Collectors.toList());
    }

    private Stream<String> mapToStream(Stream<Student> students, Function<Student, String> mapper) {
        return students.map(mapper);
    }

    private List<String> mapToList(Collection<Student> students, Function<Student, String> mapper) {
        return streamToList(mapToStream(students.stream(), mapper));
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapToList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapToList(students, Student::getGroup);
    }

    private String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapToList(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapToStream(students.stream(), Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    private <T> String getMin(Stream<T> stream, Comparator<T> comparator, Function<T, String> mapper) {
        return stream.min(comparator).map(mapper).orElse("");
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return getMin(students.stream(), Student::compareTo, Student::getFirstName);
    }

    private List<Student> sortToList(Collection<Student> students, Comparator<Student> comparator) {
        return streamToList(students.stream().sorted(comparator));
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortToList(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortToList(students, COMPARATOR_NAME);
    }

    private Stream<Student> filterStream(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate);
    }

    private List<Student> filterToList(Collection<Student> students, Predicate<Student> predicate) {
        return streamToList(filterStream(students, predicate).sorted(COMPARATOR_NAME));
    }

    private Predicate<Student> getPredicate(Function<Student, String> function, String compareString) {
        return student -> function.apply(student).equals(compareString);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterToList(students, getPredicate(Student::getFirstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterToList(students, getPredicate(Student::getLastName, name));
    }

    private Stream<Student> filterByGroup(Collection<Student> students, String group) {
        return filterStream(students, getPredicate(Student::getGroup, group));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return streamToList(filterByGroup(students, group).sorted(COMPARATOR_NAME));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return filterByGroup(students, group).collect(Collectors.toMap(
                Student::getLastName,
                Student::getFirstName,
                BinaryOperator.minBy(String::compareTo)));
    }

    private Stream<Group> groupStudents(Stream<Student> stream) {
        return stream
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet().stream()
                .map((group) -> new Group(group.getKey(), group.getValue()));
    }

    private List<Group> groupToList(Collection<Student> students, Comparator<Student> comparator) {
        return streamToList(groupStudents(students.stream().sorted(comparator)).sorted(COMPARATOR_GROUP));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return groupToList(students, COMPARATOR_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return groupToList(students, Student::compareTo);
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getMin(groupStudents(students.stream()), COMPARATOR_COUNT_OF_STUDENTS, Group::getName);
    }

    private Collector<Student, ?, Set<String>> collector(Function<Student, String> function) {
        return Collectors.mapping(function, Collectors.toSet());
    }

    private Stream<Map.Entry<String, Set<String>>> collectStudents(Collection<Student> students,
                                                                   Function<Student, String> first, Function<Student, String> second) {
        return students.stream().collect(Collectors.groupingBy(first, collector(second))).entrySet().stream();
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getMin(collectStudents(students, Student::getGroup, Student::getFirstName), COMPARATOR_FIRST_NAMES, Map.Entry::getKey);
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getMin(collectStudents(students, this::getFullName, Student::getGroup), COMPARATOR_POPULAR_NAMES.reversed(), Map.Entry::getKey);
    }

    private List<String> getStudentsByIndices(Collection<Student> students, int[] indices, Function<Student, String> mapper) {
        List<Student> list = (students instanceof List && students instanceof RandomAccess) ? (List<Student>) students : new ArrayList<>(students);
        return streamToList(IntStream.of(indices).mapToObj(list::get).map(mapper));
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getStudentsByIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getStudentsByIndices(students, indices, Student::getLastName);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getStudentsByIndices(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getStudentsByIndices(students, indices, this::getFullName);
    }
}
