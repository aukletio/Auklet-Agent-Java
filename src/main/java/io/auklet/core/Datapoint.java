package io.auklet.core;

import java.math.BigInteger;
import java.math.BigDecimal;
import io.auklet.AukletException;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.msgpack.value.ImmutableValue;
import org.msgpack.value.ValueFactory;
import net.jcip.annotations.Immutable;

import java.io.*;
import java.util.*;

/**
 * <p>Datapoints are a way to log your own custom data to the auklet backend.</p>
 */
@Immutable
public final class Datapoint {

    private static final String SIMPLE_KEY = "value";
    private final ImmutableValue value;
    private final String asString;
    private final String dataType;

    /**
     * <p>Initializes a Datapoint class which packs up the passed data into messagepack byte arrays</p>
     *
     * @param dataType string defining the type of the data to be packed
     * @param data data to be packed
     * @throws AukletException to wrap any underlying exceptions.
     */
    public Datapoint(@NonNull String dataType, @Nullable Object data) throws AukletException {
        if (dataType == null) {
            throw new AukletException("dataType cannot be null.");
        }
        if (data instanceof Datapoint) {
            Datapoint newDatapoint = (Datapoint) data;
            this.value = newDatapoint.getValue();
            this.asString = newDatapoint.toString();
            this.dataType = newDatapoint.dataType;
        } else {
            Map.Entry<String, ImmutableValue> converted = Datapoint.convertData(data).entrySet().iterator().next();
            this.value = ValueFactory.newMap(
                    ValueFactory.newString(Datapoint.SIMPLE_KEY),
                    converted.getValue()
            );
            this.asString = converted.getKey();
            this.dataType = dataType;
        }
    }

    @NonNull private static Map<String, ImmutableValue> convertData(@Nullable Object data) throws AukletException {
        Map<String, ImmutableValue> valueMap = new HashMap<>();
        if (data == null) {
            valueMap.put("null", ValueFactory.newNil());
        } else if (data.getClass().isPrimitive()) {
            String className = data.getClass().getSimpleName();
            switch (className) {
                case "Integer":
                    valueMap.put(String.valueOf((int) data),
                            ValueFactory.newInteger((int) data));
                    break;
                case "Float":
                    valueMap.put(String.valueOf((float) data),
                            ValueFactory.newFloat((float) data));
                    break;
                case "Short":
                    valueMap.put(String.valueOf((short) data),
                            ValueFactory.newInteger((short) data));
                    break;
                case "Long":
                    valueMap.put(String.valueOf((long) data),
                            ValueFactory.newInteger((long) data));
                    break;
                case "Double":
                    valueMap.put(String.valueOf((double) data),
                            ValueFactory.newFloat((double) data));
                    break;
                case "Byte":
                    valueMap.put(String.valueOf((byte) data),
                            ValueFactory.newInteger((byte) data));
                    break;
                case "Boolean":
                    valueMap.put(String.valueOf((boolean) data),
                            ValueFactory.newBoolean((boolean) data));
                    break;
                default:
                    // This is impossible
                    throw new AukletException("Error constructing Datapoint");
            }
        } else if (data instanceof Integer) {
            valueMap.put(Integer.toString((Integer) data),
                    ValueFactory.newInteger((int) data));
        } else if (data instanceof Float) {
            valueMap.put(Float.toString((Float) data),
                    ValueFactory.newFloat((float) data));
        } else if (data instanceof Short) {
            valueMap.put(Short.toString((Short) data),
                    ValueFactory.newFloat((short) data));
        } else if (data instanceof Long) {
            valueMap.put(Long.toString((Long) data),
                    ValueFactory.newInteger((long) data));
        } else if (data instanceof Double) {
            valueMap.put(Double.toString((Double) data),
                    ValueFactory.newFloat((double) data));
        } else if (data instanceof Byte) {
            valueMap.put(Byte.toString((Byte) data),
                    ValueFactory.newInteger((byte) data));
        } else if (data instanceof Boolean) {
            valueMap.put(Boolean.toString((Boolean) data),
                    ValueFactory.newBoolean((boolean) data));
        } else if (data.getClass().isArray()) {
            // construct new array of values from looping through all members
            List<ImmutableValue> newDatapoints = new ArrayList<>();
            Object[] objects = (Object[]) data;
            for (int i = 0; i < objects.length; i++) {
                Map<String, ImmutableValue> convertedMap = Datapoint.convertData(objects[i]);
                Map.Entry<String, ImmutableValue> entryMap = convertedMap.entrySet().iterator().next();
                newDatapoints.add(entryMap.getValue());
            }
            valueMap.put(
                    newDatapoints.toString(),
                    ValueFactory.newArray(newDatapoints)
            );
        } else if (data instanceof List) {
            List<ImmutableValue> newDatapoints = new ArrayList<>();
            ArrayList convertedArray = (ArrayList) data;
            for (int i = 0; i < convertedArray.size(); i++) {
                Map<String, ImmutableValue> convertedMap = Datapoint.convertData(convertedArray.get(i));
                Map.Entry<String, ImmutableValue> entryMap = convertedMap.entrySet().iterator().next();
                newDatapoints.add(entryMap.getValue());
            }
            valueMap.put(
                    newDatapoints.toString(),
                    ValueFactory.newArray(newDatapoints)
            );
        } else if (data instanceof Map) {
            Map<ImmutableValue, ImmutableValue> convertedMap = new HashMap<>();
            Map<Object, Object> castedMap = (Map) data;
            for (Map.Entry<Object, Object> entry : castedMap.entrySet()) {
                Map<String, ImmutableValue> convertedKeyMap = Datapoint.convertData(entry.getKey());
                Map.Entry<String, ImmutableValue> entryKeyMap = convertedKeyMap.entrySet().iterator().next();
                Map<String, ImmutableValue> convertedValueMap = Datapoint.convertData(entry.getValue());
                Map.Entry<String, ImmutableValue> entryValueMap = convertedValueMap.entrySet().iterator().next();
                convertedMap.put(entryKeyMap.getValue(), entryValueMap.getValue());
            }
            valueMap.put(
                    convertedMap.toString(), ValueFactory.newMap(convertedMap)
            );
        } else if (data instanceof String) {
            valueMap.put((String) data, ValueFactory.newString((String) data));
        } else if (data instanceof BigInteger) {
            BigInteger newInt = (BigInteger) data;
            valueMap.put(newInt.toString(), ValueFactory.newString(newInt.toString()));
        } else if (data instanceof BigDecimal) {
            BigDecimal newDec = (BigDecimal) data;
            valueMap.put(newDec.toString(), ValueFactory.newString(newDec.toString()));
        } else {
            throw new AukletException("We do not currently support the " + data.getClass().getName() + " class you have submitted");
        }
        return valueMap;
    }

    /**
     * <p>Returns the value converted to an ImmutableValue</p>
     *
     * @return The ImmutableValue object stored in this datapoint
     */
    @NonNull public ImmutableValue getValue() {
        return this.value;
    }

    /**
     * <p>Returns the string representation of the value stored in the datapoint</p>
     *
     * @return String representation of the current value of the datapoint
     */
    @Override @NonNull public String toString() {
        return this.asString;
    }

    /**
     * <p>Returns the string which describes the data stored within the datapoint</p>
     * @return User defined string which describes the type of data within the datapoint
     */
    @NonNull public String getDataType() {
        return this.dataType;
    }
}