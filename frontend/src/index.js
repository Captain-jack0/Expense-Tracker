import { AppRegistry } from 'react-native';
import App from './App';

AppRegistry.registerComponent('ExpenseTracker', () => App);

if (typeof document !== 'undefined') {
  const rootTag = document.getElementById('root');
  AppRegistry.runApplication('ExpenseTracker', { rootTag });
}
