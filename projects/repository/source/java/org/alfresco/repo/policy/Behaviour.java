package org.alfresco.repo.policy;


/**
 * A Behaviour represents an encapsulated piece of logic (system or business)
 * that may be bound to a Policy.  The logic may be expressed in any
 * language (java, script etc).  
 *
 * Once bound to a Policy, the behaviour must be able to provide the interface
 * declared by that policy.
 * 
 * @author David Caruana
 */
public interface Behaviour
{
    /**
     * Gets the requested policy interface onto the behaviour 
     * 
     * @param policy  the policy interface class
     * @return  the policy interface
     */
    public <T> T getInterface(Class<T> policy);
    
    /**
     * Disable the behaviour (for this thread only)
     */
    public void disable();

    /**
     * Enable the behaviour (for this thread only)
     */
    public void enable();

    /**
     * @return  is the behaviour enabled (for this thread only)
     */
    public boolean isEnabled();
    
}
