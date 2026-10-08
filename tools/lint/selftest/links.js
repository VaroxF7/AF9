// Self-test fixture: links to Java that are broken
const $SelftestNoClass = Java.loadClass('com.af9.core.machine.SelftestNoSuchClass')               // J1
const $SelftestStation = Java.loadClass('com.af9.core.machine.LithoMachine')
const selftestMember = $SelftestStation.SELFTEST_NO_SUCH_FIELD                                      // J2
