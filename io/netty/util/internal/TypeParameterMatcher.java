package io.netty.util.internal;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

















public abstract class TypeParameterMatcher
{
  private static final TypeParameterMatcher NOOP = new TypeParameterMatcher()
  {
    public boolean match(Object msg) {
      return true;
    }
  };
  
  public static TypeParameterMatcher get(Class<?> parameterType)
  {
    Map<Class<?>, TypeParameterMatcher> getCache = InternalThreadLocalMap.get().typeParameterMatcherGetCache();
    
    TypeParameterMatcher matcher = (TypeParameterMatcher)getCache.get(parameterType);
    if (matcher == null) {
      if (parameterType == Object.class) {
        matcher = NOOP;
      } else {
        matcher = new ReflectiveMatcher(parameterType);
      }
      getCache.put(parameterType, matcher);
    }
    
    return matcher;
  }
  


  public static TypeParameterMatcher find(Object object, Class<?> parametrizedSuperclass, String typeParamName)
  {
    Map<Class<?>, Map<String, TypeParameterMatcher>> findCache = InternalThreadLocalMap.get().typeParameterMatcherFindCache();
    Class<?> thisClass = object.getClass();
    
    Map<String, TypeParameterMatcher> map = (Map)findCache.get(thisClass);
    if (map == null) {
      map = new HashMap();
      findCache.put(thisClass, map);
    }
    
    TypeParameterMatcher matcher = (TypeParameterMatcher)map.get(typeParamName);
    if (matcher == null) {
      matcher = get(find0(object, parametrizedSuperclass, typeParamName));
      map.put(typeParamName, matcher);
    }
    
    return matcher;
  }
  

  private static Class<?> find0(Object object, Class<?> parametrizedSuperclass, String typeParamName)
  {
    Class<?> thisClass = object.getClass();
    Class<?> currentClass = thisClass;
    do {
      while (currentClass.getSuperclass() == parametrizedSuperclass) {
        int typeParamIndex = -1;
        TypeVariable<?>[] typeParams = currentClass.getSuperclass().getTypeParameters();
        for (int i = 0; i < typeParams.length; i++) {
          if (typeParamName.equals(typeParams[i].getName())) {
            typeParamIndex = i;
            break;
          }
        }
        
        if (typeParamIndex < 0) {
          throw new IllegalStateException("unknown type parameter '" + typeParamName + "': " + parametrizedSuperclass);
        }
        

        Type genericSuperType = currentClass.getGenericSuperclass();
        if (!(genericSuperType instanceof ParameterizedType)) {
          return Object.class;
        }
        
        Type[] actualTypeParams = ((ParameterizedType)genericSuperType).getActualTypeArguments();
        
        Type actualTypeParam = actualTypeParams[typeParamIndex];
        if ((actualTypeParam instanceof ParameterizedType)) {
          actualTypeParam = ((ParameterizedType)actualTypeParam).getRawType();
        }
        if ((actualTypeParam instanceof Class)) {
          return (Class)actualTypeParam;
        }
        if ((actualTypeParam instanceof GenericArrayType)) {
          Type componentType = ((GenericArrayType)actualTypeParam).getGenericComponentType();
          if ((componentType instanceof ParameterizedType)) {
            componentType = ((ParameterizedType)componentType).getRawType();
          }
          if ((componentType instanceof Class)) {
            return Array.newInstance((Class)componentType, 0).getClass();
          }
        }
        if ((actualTypeParam instanceof TypeVariable))
        {
          TypeVariable<?> v = (TypeVariable)actualTypeParam;
          if (!(v.getGenericDeclaration() instanceof Class)) {
            return Object.class;
          }
          
          currentClass = thisClass;
          parametrizedSuperclass = (Class)v.getGenericDeclaration();
          typeParamName = v.getName();
          if (!parametrizedSuperclass.isAssignableFrom(thisClass))
          {

            return Object.class;
          }
        } else {
          return fail(thisClass, typeParamName);
        } }
      currentClass = currentClass.getSuperclass();
    } while (currentClass != null);
    return fail(thisClass, typeParamName);
  }
  

  private static Class<?> fail(Class<?> type, String typeParamName)
  {
    throw new IllegalStateException("cannot determine the type of the type parameter '" + typeParamName + "': " + type);
  }
  
  public abstract boolean match(Object paramObject);
  
  TypeParameterMatcher() {}
  
  private static final class ReflectiveMatcher extends TypeParameterMatcher {
    private final Class<?> type;
    
    ReflectiveMatcher(Class<?> type) { this.type = type; }
    

    public boolean match(Object msg)
    {
      return type.isInstance(msg);
    }
  }
}
