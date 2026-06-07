/**
 * Server-side triggers for client-rendered Home-Horror illusions (GDD §7/§10). Each sends a
 * unicast {@link com.denfry.eidolondrift.network.TriggerClientAnomalyPayload} (invariant §2);
 * the client builds a self-expiring instance and the server never tracks what is drawn
 * (invariant §3). M3 ships the feasible visuals (shadow, desaturate); M4 broadens the set.
 */
package com.denfry.eidolondrift.anomaly.client;
